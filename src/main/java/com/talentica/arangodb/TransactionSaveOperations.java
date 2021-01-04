package com.talentica.arangodb;

import lombok.Data;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import java.sql.Connection;
import java.sql.Savepoint;
import java.util.LinkedList;
import java.util.List;

@Data
public class TransactionSaveOperations implements TransactionStatus {
    private List<Object> documents = new LinkedList<>();
    private boolean newTrans = true;
    private boolean rollbackOnly;
    private boolean completed;
    private String id = "tx_"+Thread.currentThread().getId()+System.currentTimeMillis();


    public String getId(){
        return id;
    }

    /**
     * Return whether this transaction internally carries a savepoint,
     * that is, has been created as nested transaction based on a savepoint.
     * <p>This method is mainly here for diagnostic purposes, alongside
     * {@link #isNewTransaction()}. For programmatic handling of custom
     * savepoints, use the operations provided by {@link SavepointManager}.
     *
     * @see #isNewTransaction()
     * @see #createSavepoint()
     * @see #rollbackToSavepoint(Object)
     * @see #releaseSavepoint(Object)
     */
    @Override
    public boolean hasSavepoint() {
        return false;
    }

    /**
     * Flush the underlying session to the datastore, if applicable:
     * for example, all affected Hibernate/JPA sessions.
     * <p>This is effectively just a hint and may be a no-op if the underlying
     * transaction manager does not have a flush concept. A flush signal may
     * get applied to the primary resource or to transaction synchronizations,
     * depending on the underlying resource.
     */
    @Override
    public void flush() {
        //no-ops
    }

    /**
     * Create a new savepoint. You can roll back to a specific savepoint
     * via {@code rollbackToSavepoint}, and explicitly release a savepoint
     * that you don't need anymore via {@code releaseSavepoint}.
     * <p>Note that most transaction managers will automatically release
     * savepoints at transaction completion.
     *
     * @return a savepoint object, to be passed into
     * {@link #rollbackToSavepoint} or {@link #releaseSavepoint}
     * @throws NestedTransactionNotSupportedException if the underlying
     *                                                transaction does not support savepoints
     * @throws TransactionException                   if the savepoint could not be created,
     *                                                for example because the transaction is not in an appropriate state
     * @see Connection#setSavepoint
     */
    @Override
    public Object createSavepoint() throws TransactionException {
        return null;
    }

    /**
     * Roll back to the given savepoint.
     * <p>The savepoint will <i>not</i> be automatically released afterwards.
     * You may explicitly call {@link #releaseSavepoint(Object)} or rely on
     * automatic release on transaction completion.
     *
     * @param savepoint the savepoint to roll back to
     * @throws NestedTransactionNotSupportedException if the underlying
     *                                                transaction does not support savepoints
     * @throws TransactionException                   if the rollback failed
     * @see Connection#rollback(Savepoint)
     */
    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {

    }

    /**
     * Explicitly release the given savepoint.
     * <p>Note that most transaction managers will automatically release
     * savepoints on transaction completion.
     * <p>Implementations should fail as silently as possible if proper
     * resource cleanup will eventually happen at transaction completion.
     *
     * @param savepoint the savepoint to release
     * @throws NestedTransactionNotSupportedException if the underlying
     *                                                transaction does not support savepoints
     * @throws TransactionException                   if the release failed
     * @see Connection#releaseSavepoint
     */
    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {

    }

    /**
     * Return whether the present transaction is new; otherwise participating
     * in an existing transaction, or potentially not running in an actual
     * transaction in the first place.
     */
    @Override
    public boolean isNewTransaction() {
        return newTrans;
    }

    /**
     * Set the transaction rollback-only. This instructs the transaction manager
     * that the only possible outcome of the transaction may be a rollback, as
     * alternative to throwing an exception which would in turn trigger a rollback.
     */
    @Override
    public void setRollbackOnly() {
        rollbackOnly=true;
    }

    /**
     * Return whether the transaction has been marked as rollback-only
     * (either by the application or by the transaction infrastructure).
     */
    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    /**
     * Return whether this transaction is completed, that is,
     * whether it has already been committed or rolled back.
     */
    @Override
    public boolean isCompleted() {
        return completed;
    }
}
