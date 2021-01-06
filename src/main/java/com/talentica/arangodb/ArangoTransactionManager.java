package com.talentica.arangodb;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Entity;
import com.arangodb.internal.util.ArangoSerializationFactory;
import com.arangodb.model.*;
import com.arangodb.springframework.annotation.*;
import com.arangodb.springframework.config.ArangoConfiguration;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.CollectionOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoEntityWriter;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.template.ArangoTemplate;
import com.arangodb.springframework.core.template.DefaultCollectionOperations;
import com.arangodb.springframework.core.util.ArangoExceptionTranslator;
import com.arangodb.util.ArangoSerialization;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocystream.Response;
import com.talentica.arangodb.annotation.CustomId;
import com.talentica.arangodb.idprovider.IdProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.transaction.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@ComponentScan(basePackages = "com.talentica.arangodb")
public class ArangoTransactionManager implements PlatformTransactionManager {
    private static final String REPSERT_QUERY =
            "result[%d]=db._query(\"LET doc = %s UPSERT { _key: doc._key } " +
                    "INSERT doc._key == null ? UNSET(doc, \\\"_key\\\") : doc " +
                    "REPLACE doc " +
                    "IN %s " +
                    "OPTIONS { ignoreRevs: false } " +
                    "RETURN NEW\")['_documents'][0];\n";

    private final Map<Long, TransactionSaveOperations> transactionSaveOperationsMap= new ConcurrentHashMap<>();
    private final Map<CollectionCacheKey, CollectionCacheValue> collectionCache = new ConcurrentHashMap<>();
    private final Map<Class,Field> customIdFieldMap = new ConcurrentHashMap<>();
    private final Map<Class,IdProvider> customIdFieldProviderMap = new ConcurrentHashMap<>();

    private ArangoExceptionTranslator exceptionTranslator =new ArangoExceptionTranslator();

    private MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> mappingContext;

    private ArangoOperations arangoOperations;

    private ArangoEntityWriter writer;

    private ArangoConverter converter;

    private ArangoConfiguration arangoConfiguration;

    public ArangoTransactionManager( ArangoOperations arangoOperations, ArangoEntityWriter writer, ArangoConverter converter, ArangoConfiguration arangoConfiguration) {
        this.mappingContext = converter.getMappingContext();
        this.arangoOperations = arangoOperations;
        this.writer = writer;
        this.converter = converter;
        this.arangoConfiguration = arangoConfiguration;
    }

    @Data
    @AllArgsConstructor
    protected static class FieldAndProvider{
        private Field field;
        private IdProvider provider;
    }
    protected FieldAndProvider findCustomIdField(Object entity) {
        Class<?> cl = entity.getClass();

        Field idField = customIdFieldMap.get(cl);
        IdProvider provider = customIdFieldProviderMap.get(cl);
        if(idField!=null){

            return new FieldAndProvider(idField, provider);
        }

        while(cl!=Object.class){
            var fields = cl.getDeclaredFields();
            for(var f:fields){
                var annotation = f.getAnnotation(CustomId.class);
                if(annotation!=null){
                    if(idField!=null){
                        throw new IllegalTransactionStateException("Multiple Custom ID fields not supported for the same entiry");
                    }
                    idField = f;
                    try {
                        provider = annotation.provider().getConstructor(new Class[0]).newInstance();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    customIdFieldMap.put(cl,idField);
                    customIdFieldProviderMap.put(cl,provider);
                }
            }
            cl=cl.getSuperclass();
        }
        if(idField!=null){
            return new FieldAndProvider(idField,provider);
        }else{
            return null;
        }

    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        if(definition.getIsolationLevel()!=TransactionDefinition.ISOLATION_READ_COMMITTED&&
                definition.getIsolationLevel()!=TransactionDefinition.ISOLATION_DEFAULT){
            throw new IllegalTransactionStateException("Only read-committed isolation is supported");
        }
        if(definition.getPropagationBehavior()==TransactionDefinition.PROPAGATION_NESTED ||
                definition.getPropagationBehavior()==TransactionDefinition.PROPAGATION_REQUIRES_NEW){
            throw new IllegalTransactionStateException("Unsupported transaction propagation");
        }
        var status = transactionSaveOperationsMap.get(Thread.currentThread().getId());
        if(status==null){
            status = new TransactionSaveOperations();
            transactionSaveOperationsMap.put(Thread.currentThread().getId(), status);
        }else{
            status.setNewTrans(false);
        }
        return status;
    }

    public <T> void saveToTransaction(T entity){
        var thread = Thread.currentThread().getId();
        var saveOperations = transactionSaveOperationsMap.get(thread);

        var cusomIdMeta = findCustomIdField(entity);
        if(cusomIdMeta!=null){
            var field = cusomIdMeta.getField();
            var provider = cusomIdMeta.getProvider();
            field.setAccessible(true);
            try {
                var value = field.get(entity);
                if(value==null) {
                    field.set(entity, provider.newUniqueID(field.getType(), entity));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        saveOperations.getDocuments().add(entity);

    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        //write code to commit to the database
        var thread = Thread.currentThread().getId();
        var saveOperations = transactionSaveOperationsMap.get(thread);
        StringBuilder actionSb = new StringBuilder("function(){\n "
                +"var result = [];"
                +"db = require(\"@arangodb\").db; \n");
        var tOpts = new TransactionOptions();
        List<String> writeCollections = new LinkedList<>();
        if(saveOperations!=null){
            var docs = saveOperations.getDocuments();
            for(var i=0;i<docs.size();i++){
                var entity = docs.get(i);
                var collection = _collection(entity.getClass());
                var vpacSlice = writer.write(entity);
                var escapedData = StringEscapeUtils.escapeJson(vpacSlice.toString());
                var query = String.format(REPSERT_QUERY,i, escapedData, collection.name());
                actionSb.append(query);
                writeCollections.add(collection.name());

            }
        }
        actionSb.append("return result; \n }");
        tOpts.writeCollections(writeCollections.toArray(new String[]{}));
        tOpts.waitForSync(true);
        String action = actionSb.toString();
        var result = arangoOperations.driver().db(arangoConfiguration.database())
                .transaction(action, VPackSlice.class, tOpts);
        var resList = converter.read(List.class, result);
        var docs = saveOperations.getDocuments();
        for(var i=0;i<docs.size();i++) {
            var entity = docs.get(i);
            var returnedEntity = resList.get(i);
            BeanUtils.copyProperties(returnedEntity, entity);
        }
        transactionSaveOperationsMap.remove(Thread.currentThread().getId());
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        transactionSaveOperationsMap.remove(Thread.currentThread().getId());
    }

    private ArangoCollection _collection(final Class<?> entityClass) {
        final ArangoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(entityClass);
        final String name = persistentEntity.getCollection();
        return _collection(name, persistentEntity, persistentEntity.getCollectionOptions());
    }

    private ArangoCollection _collection(final String name, final ArangoPersistentEntity<?> persistentEntity,
                                         final CollectionCreateOptions options) {

        final ArangoDatabase db = arangoOperations.driver().db();
        final Class<?> entityClass = persistentEntity != null ? persistentEntity.getType() : null;
        final CollectionCacheValue value = collectionCache.computeIfAbsent(new CollectionCacheKey(db.name(), name),
                key -> {
                    final ArangoCollection collection = db.collection(name);
                    if (!collection.exists()) {
                        collection.create(options);
                    }
                    return new CollectionCacheValue(collection);
                });
        final Collection<Class<?>> entities = value.getEntities();
        final ArangoCollection collection = value.getCollection();
        if (persistentEntity != null && !entities.contains(entityClass)) {
            value.addEntityClass(entityClass);
            ensureCollectionIndexes(collection(collection.name()), persistentEntity);
            ensureCustomIDIndexes(collection(collection.name()), persistentEntity);
        }
        return collection;
    }
    private CollectionOperations collection(final String name) {
        return arangoOperations.collection(name);
    }

    private static void ensureCustomIDIndexes(final CollectionOperations collection, final ArangoPersistentEntity<?> persistentEntity){
        var props = persistentEntity.getPersistentProperties(CustomId.class);
        props.forEach(index -> ensureUniquePersistentIndex(collection, index));
    }
    private static void ensureCollectionIndexes(final CollectionOperations collection,
                                                final ArangoPersistentEntity<?> persistentEntity) {
        persistentEntity.getHashIndexes().stream().forEach(index -> ensureHashIndex(collection, index));
        persistentEntity.getHashIndexedProperties().stream().forEach(p -> ensureHashIndex(collection, p));
        persistentEntity.getSkiplistIndexes().stream().forEach(index -> ensureSkiplistIndex(collection, index));
        persistentEntity.getSkiplistIndexedProperties().stream().forEach(p -> ensureSkiplistIndex(collection, p));
        persistentEntity.getPersistentIndexes().stream().forEach(index -> ensurePersistentIndex(collection, index));
        persistentEntity.getPersistentIndexedProperties().stream().forEach(p -> ensurePersistentIndex(collection, p));
        persistentEntity.getGeoIndexes().stream().forEach(index -> ensureGeoIndex(collection, index));
        persistentEntity.getGeoIndexedProperties().stream().forEach(p -> ensureGeoIndex(collection, p));
        persistentEntity.getFulltextIndexes().stream().forEach(index -> ensureFulltextIndex(collection, index));
        persistentEntity.getFulltextIndexedProperties().stream().forEach(p -> ensureFulltextIndex(collection, p));
    }

    private static void ensureHashIndex(final CollectionOperations collection, final HashIndex annotation) {
        collection.ensureHashIndex(Arrays.asList(annotation.fields()), new HashIndexOptions()
                .unique(annotation.unique()).sparse(annotation.sparse()).deduplicate(annotation.deduplicate()));
    }

    private static void ensureHashIndex(final CollectionOperations collection, final ArangoPersistentProperty value) {
        final HashIndexOptions options = new HashIndexOptions();
        value.getHashIndexed()
                .ifPresent(i -> options.unique(i.unique()).sparse(i.sparse()).deduplicate(i.deduplicate()));
        collection.ensureHashIndex(Collections.singleton(value.getFieldName()), options);
    }

    private static void ensureSkiplistIndex(final CollectionOperations collection, final SkiplistIndex annotation) {
        collection.ensureSkiplistIndex(Arrays.asList(annotation.fields()), new SkiplistIndexOptions()
                .unique(annotation.unique()).sparse(annotation.sparse()).deduplicate(annotation.deduplicate()));
    }

    private static void ensureSkiplistIndex(final CollectionOperations collection,
                                            final ArangoPersistentProperty value) {
        final SkiplistIndexOptions options = new SkiplistIndexOptions();
        value.getSkiplistIndexed()
                .ifPresent(i -> options.unique(i.unique()).sparse(i.sparse()).deduplicate(i.deduplicate()));
        collection.ensureSkiplistIndex(Collections.singleton(value.getFieldName()), options);
    }

    private static void ensurePersistentIndex(final CollectionOperations collection, final PersistentIndex annotation) {
        collection.ensurePersistentIndex(Arrays.asList(annotation.fields()),
                new PersistentIndexOptions().unique(annotation.unique()).sparse(annotation.sparse()));
    }

    private static void ensurePersistentIndex(final CollectionOperations collection,
                                              final ArangoPersistentProperty value) {
        final PersistentIndexOptions options = new PersistentIndexOptions();
        value.getPersistentIndexed().ifPresent(i -> options.unique(i.unique()).sparse(i.sparse()));
        collection.ensurePersistentIndex(Collections.singleton(value.getFieldName()), options);
    }
    private static void ensureUniquePersistentIndex(final CollectionOperations collection,
                                              final ArangoPersistentProperty value) {
        final PersistentIndexOptions options = new PersistentIndexOptions();
        options.unique(true);
        options.sparse(false);
        collection.ensurePersistentIndex(Collections.singleton(value.getFieldName()), options);
    }

    private static void ensureGeoIndex(final CollectionOperations collection, final GeoIndex annotation) {
        collection.ensureGeoIndex(Arrays.asList(annotation.fields()),
                new GeoIndexOptions().geoJson(annotation.geoJson()));
    }

    private static void ensureGeoIndex(final CollectionOperations collection, final ArangoPersistentProperty value) {
        final GeoIndexOptions options = new GeoIndexOptions();
        value.getGeoIndexed().ifPresent(i -> options.geoJson(i.geoJson()));
        collection.ensureGeoIndex(Collections.singleton(value.getFieldName()), options);
    }

    private static void ensureFulltextIndex(final CollectionOperations collection, final FulltextIndex annotation) {
        collection.ensureFulltextIndex(Collections.singleton(annotation.field()),
                new FulltextIndexOptions().minLength(annotation.minLength() > -1 ? annotation.minLength() : null));
    }

    private static void ensureFulltextIndex(final CollectionOperations collection,
                                            final ArangoPersistentProperty value) {
        final FulltextIndexOptions options = new FulltextIndexOptions();
        value.getFulltextIndexed().ifPresent(i -> options.minLength(i.minLength() > -1 ? i.minLength() : null));
        collection.ensureFulltextIndex(Collections.singleton(value.getFieldName()), options);
    }


}
