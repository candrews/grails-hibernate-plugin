package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 27, 2008
 */
class CriteriaBuilderTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class CriteriaBuilderBook {
    String title
    static belongsTo = [author:CriteriaBuilderAuthor]

    static mapping = {
        cache true
    }
}

@Entity
class CriteriaBuilderAuthor {

    String name
    static hasMany = [books:CriteriaBuilderBook]

    static mapping = {
        cache true
    }
}
'''
    }

    void testIdEq() {
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz
        def bookClass = ga.getDomainClass("CriteriaBuilderBook").clazz

        assertNotNull authorClass.newInstance(name:"Stephen King")
                                 .addToBooks(title:"The Shining")
                                 .addToBooks(title:"The Stand")
                                 .addToBooks(title:"Rose Madder")
                                 .save(flush:true)

        assertNotNull authorClass.newInstance(name:"James Patterson")
                                 .addToBooks(title:"Along Came a Spider")
                                 .addToBooks(title:"A Time to Kill")
                                 .addToBooks(title:"Killing Me Softly")
                                 .addToBooks(title:"The Quickie")
                                 .save(flush:true)

        session.clear()
        def book = bookClass.findByTitle("The Quickie")

        assertNotNull "should have found book", book

        def authors = authorClass.withCriteria {
            books {
                idEq book.id
            }
        }

        assertNotNull "should have returned a list of authors", authors
        assertEquals 1, authors.size()
        assertEquals "James Patterson", authors[0].name
    }

    void testSizeCriterion() {
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        assertNotNull authorClass.newInstance(name:"Stephen King")
                                 .addToBooks(title:"The Shining")
                                 .addToBooks(title:"The Stand")
                                 .addToBooks(title:"Rose Madder")
                                 .save(flush:true)

        assertNotNull authorClass.newInstance(name:"James Patterson")
                                 .addToBooks(title:"Along Came a Spider")
                                 .addToBooks(title:"A Time to Kill")
                                 .addToBooks(title:"Killing Me Softly")
                                 .addToBooks(title:"The Quickie")
                                 .save(flush:true)

        def results = authorClass.withCriteria {
            sizeGt('books', 3)
        }
        assertEquals 1, results.size()

        results = authorClass.withCriteria {
            sizeGe('books', 3)
        }
        assertEquals 2, results.size()

        results = authorClass.withCriteria {
            sizeNe('books', 1)
        }
        assertEquals 2, results.size()

        results = authorClass.withCriteria {
            sizeNe('books', 3)
        }
        assertEquals 1, results.size()

        results = authorClass.withCriteria {
            sizeLt('books', 4)
        }
        assertEquals 1, results.size()

        results = authorClass.withCriteria {
            sizeLe('books', 4)
        }
        assertEquals 2, results.size()
    }

    void testCacheMethod() {
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
                                .addToBooks(title:"The Shining")
                                .addToBooks(title:"The Stand")
                                .save(flush:true)

        assertNotNull author

        session.clear()

        def authors = authorClass.withCriteria {
            eq('name', 'Stephen King')

            def criteriaInstance = getInstance()
            assertTrue criteriaInstance.cacheable
        }

        assertEquals 1, authors.size()

        // NOTE: note sure how to actually test the cache, I'm testing
        // that invoking the cache method works but need a better test
        // that ensure entries are pulled from the cache

        authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
            cache false

            def criteriaInstance = getInstance()
            assertFalse criteriaInstance.cacheable
        }

        assertEquals 1, authors.size()
    }

    void testLockMethod() {

        // NOTE: HSQLDB doesn't support the SQL SELECT..FOR UPDATE syntax so this test
        // is basically just testing that the lock method can be called without error

        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
                                .addToBooks(title:"The Shining")
                                .addToBooks(title:"The Stand")
                                .save(flush:true)
        assertNotNull author

        session.clear()

        def authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
            lock true
        }

        assert authors
    
        // test lock association

        try {
            authors = authorClass.withCriteria {
                eq('name', 'Stephen King')
                books {
                    lock true
                }
            }
    
            assert authors
        } catch (Exception e) {
            // workaround for h2 issue https://code.google.com/p/h2database/issues/detail?id=541
            if(!e.cause?.message?.contains("Feature not supported")) {
                throw e
            }
        }
    }

    void testJoinMethod() {
        def authorClass = ga.getDomainClass("CriteriaBuilderAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
                                .addToBooks(title:"The Shining")
                                .addToBooks(title:"The Stand")
                                .save(flush:true)

        assertNotNull author

        session.clear()

        def authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
        }
        assert authors
        author = authors[0]

        assertFalse "books association is lazy by default and shouldn't be initialized",Hibernate.isInitialized(author.books)

        session.clear()

        authors = authorClass.withCriteria {
            eq('name', 'Stephen King')
            join "books"
        }
        author = authors[0]

        assertTrue "books association loaded with join query and should be initialized",Hibernate.isInitialized(author.books)
    }
}
