package com.talentica.arangodb2.dataloader;

import com.talentica.arangodb2.entity.Book;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BookRdfParser implements BookDataParser {
    static final String NS_BASE = "http://www.gutenberg.org/";
    static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    static final String NS_DCTERMS = "http://purl.org/dc/terms/";
    static final String NS_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    static final String NS_PGTERMS = "http://www.gutenberg.org/2009/pgterms/";
    static final String NS_CC = "http://web.resource.org/cc/";
    static final String NS_DCAM = "http://purl.org/dc/dcam/";

    private final File directory;

    public BookRdfParser(File directory) {
        this.directory = directory;
    }

    @Override
    public Iterable<Book> parseBook() throws DataParserException {

        return new BookRdfIterable();
    }

    public Book parseBook(InputStream in) throws XMLStreamException, DataParserException {
        var xmlInputFactory = XMLInputFactory.newInstance();
        var xmlEventReader = xmlInputFactory.createXMLEventReader(in);
        Book book = new Book();
        List<String> subjects = new ArrayList<>();
        List<Book.Format> formats = new ArrayList<>();
        List<Book.Creator> creators = new ArrayList<>();
        List<String> bookshelves = new ArrayList<>();
        while (xmlEventReader.hasNext()) {
            var e = xmlEventReader.nextEvent();
            if (e.isStartElement() && e.asStartElement().getName().equals(new QName(NS_PGTERMS, "ebook"))) {
                break;
            }
        }
        while (xmlEventReader.hasNext()) {
            var event = xmlEventReader.nextEvent();
            if (event.isStartElement()) {
                var elem = event.asStartElement();
                if (elem.getName().equals(new QName(NS_DCTERMS, "subject"))) {
                    String subject = parseInternalDescription(event, xmlEventReader);
                    subjects.add(subject);
                } else if (elem.getName().equals(new QName(NS_PGTERMS, "bookshelf"))) {
                    String bookshelf = parseInternalDescription(event, xmlEventReader);
                    bookshelves.add(bookshelf);
                } else if (elem.getName().equals(new QName(NS_DCTERMS, "hasFormat"))) {
                    var format = parseFormat(event, xmlEventReader);
                    formats.add(format);
                } else if (elem.getName().equals(new QName(NS_DCTERMS, "creator"))) {
                    var creator = parseCreator(event, xmlEventReader);
                    creators.add(creator);
                } else if (elem.getName().equals(new QName(NS_DCTERMS, "title"))) {
                    var title = parseElementContent(event, xmlEventReader);
                    book.setTitle(title);
                } else if (elem.getName().equals(new QName(NS_PGTERMS, "downloads"))) {
                    var downloads = parseElementContent(event, xmlEventReader);
                    book.setDownloads(Long.parseLong(downloads));
                } else {
                    ignoreElement(xmlEventReader);
                }
            } else if (event.isEndElement()) {
                break;
            }

        }
        book.setSubjects(subjects.toArray(String[]::new));
        book.setFormats(formats.toArray(Book.Format[]::new));
        book.setCreators(creators.toArray(Book.Creator[]::new));
        book.setBookshelves(bookshelves.toArray(String[]::new));
        return book;
    }


    private Book.Creator parseCreator(XMLEvent event, XMLEventReader reader) throws XMLStreamException, DataParserException {
        var creator = new Book.Creator();
        var agentEvent = reader.nextEvent();
        while (!agentEvent.isStartElement()) {
            agentEvent = reader.nextEvent();
        }
        if (agentEvent.asStartElement().getName().equals(new QName(NS_PGTERMS, "agent"))) {
            while (reader.hasNext()) {
                var ev = reader.nextEvent();
                if (ev.isEndElement()) { // break out of the has file element
                    break;
                } else if (ev.isStartElement()) {
                    var elem = ev.asStartElement();
                    if (elem.getName().equals(new QName(NS_PGTERMS, "deathdate"))) {
                        var dateStr = parseElementContent(ev, reader);
                        creator.setDeathDate(Integer.parseInt(dateStr));

                    } else if (elem.getName().equals(new QName(NS_PGTERMS, "birthdate"))) {
                        var dateStr = parseElementContent(ev, reader);
                        creator.setBirthDate(Integer.parseInt(dateStr));
                    } else if (elem.getName().equals(new QName(NS_PGTERMS, "alias"))) {
                        var alias = parseElementContent(event, reader);
                        creator.setAlias(alias);
                    } else if (elem.getName().equals(new QName(NS_PGTERMS, "name"))) {
                        var name = parseElementContent(event, reader);
                        creator.setName(name);
                    } else if (elem.getName().equals(new QName(NS_PGTERMS, "webpage"))) {
                        var webpage
                                = elem
                                .getAttributeByName(new QName(NS_RDF, "resource")).getValue();
                        creator.setWebpage(webpage);
                        ignoreElement(reader);
                    } else {
                        ignoreElement(reader);
                    }
                }
            }
        }
        ignoreElement(reader);
        return creator;
    }

    private Book.Format parseFormat(XMLEvent event, XMLEventReader reader) throws XMLStreamException, DataParserException {
        var fileEvent = reader.nextEvent();
        while (!fileEvent.isStartElement()) {
            fileEvent = reader.nextEvent();
        }
        var format = new Book.Format();
        if (fileEvent.isStartElement()
                && fileEvent.asStartElement().getName().equals(new QName(NS_PGTERMS, "file"))) {
            var fileElement = fileEvent.asStartElement();
            var url = fileElement.getAttributeByName(new QName(NS_RDF, "about")).getValue();
            format.setUrl(url);
            while (reader.hasNext()) {
                var ev = reader.nextEvent();
                if (ev.isEndElement()) { // break out of the has file element
                    break;
                } else if (ev.isStartElement()) {
                    var elem = ev.asStartElement();
                    if (elem.getName().equals(new QName(NS_DCTERMS, "format"))) {
                        var descriptionEv = reader.nextEvent();
                        while (!descriptionEv.isStartElement()) {
                            descriptionEv = reader.nextEvent();
                        }
                        String fm = parseDescription(reader);
                        ignoreElement(reader);
                        format.setFormat(fm);
                    } else if (elem.getName().equals(new QName(NS_DCTERMS, "modified"))) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        String dateString = parseElementContent(ev, reader);
                        try {
                            Date date = sdf.parse(dateString);
                            format.setModified(date);
                        } catch (ParseException e) {
                            throw new DataParserException(e);
                        }
                    } else if (elem.getName().equals(new QName(NS_DCTERMS, "extent"))) {
                        long extent = Long.parseLong(parseElementContent(ev, reader));
                        format.setExtent(extent);
                    } else {
                        ignoreElement(reader);
                    }
                }
            }

        }
        ignoreElement(reader);
        return format;

    }

    private String parseInternalDescription(XMLEvent event, XMLEventReader reader) throws XMLStreamException, DataParserException {
        var descriptionEvent = reader.nextEvent();
        while (!descriptionEvent.isStartElement()) {
            descriptionEvent = reader.nextEvent();
        }
        String subject = null;
        if (descriptionEvent.isStartElement()
                && descriptionEvent.asStartElement().getName().equals(new QName(NS_RDF, "Description"))) {
            subject = parseDescription(reader);
        }

        ignoreElement(reader);

        return subject;
    }

    private String parseDescription(XMLEventReader reader) throws XMLStreamException, DataParserException {
        String subject = null;
        while (reader.hasNext()) {
            var ev = reader.nextEvent();
            if (ev.isStartElement()) {
                if (ev.asStartElement().getName().equals(new QName(NS_RDF, "value"))) {
                    subject = parseElementContent(ev, reader);
                } else {
                    ignoreElement(reader);
                }
            } else if (ev.isEndElement()) {// this is the end of the description element.
                break;
            }
        }
        return subject;
    }

    /**
     * Used to ignore the rest of the current element represented by the event
     *
     * @param reader
     * @throws XMLStreamException
     * @throws DataParserException
     */
    private void ignoreElement(XMLEventReader reader) throws XMLStreamException, DataParserException {
        while (reader.hasNext()) {
            var ev = reader.nextEvent();
            if (ev.isEndElement()) {
                return;
            } else if (ev.isStartElement()) {
                ignoreElement(reader);
            }
        }
    }

    /**
     * Read the contents of an element represented by the event.
     *
     * @param event
     * @param reader
     * @return The text content of a text element
     * @throws XMLStreamException
     * @throws DataParserException
     */
    private String parseElementContent(XMLEvent event, XMLEventReader reader) throws XMLStreamException, DataParserException {
        var title = new StringBuilder();
        if (event.isStartElement()) {

            var element = event.asStartElement();

            while (reader.hasNext()) {
                var ev = reader.nextEvent();
                if (ev.isCharacters()) {
                    title.append(ev.asCharacters().getData());
                } else if (ev.isEndElement()) {
                    break;
                } else {
                    throw new DataParserException("Unexpected parsing event " + ev);
                }
            }

        }
        return title.toString();
    }


    class BookRdfIterator implements Iterator<Book> {
        Stack<File> files = new Stack<>();
        File next = null;

        public BookRdfIterator(){
            files.push(directory);
            updateNext();
        }

        @Override
        public boolean hasNext() {
            return next!=null;
        }

        @Override
        public Book next() {
            var ret = next;
            updateNext();
            try {
                return parseBook(new FileInputStream(ret));
            }  catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void updateNext() {
            while(true){
                if(files.isEmpty()){
                    next =  null;
                    break;
                }
                var f = files.pop();
                if(f.isDirectory()){
                    var members = f.list();
                    for(var member:members){
                        files.push(new File(f,member));
                    }
                }else{
                    try {
                       next = f;
                       break;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

    }

    class BookRdfIterable implements Iterable<Book> {

        @Override
        public Iterator<Book> iterator() {
            return new BookRdfIterator();
        }
    }
}
