package org.springframework.mock.jndi;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-repo shim of Spring's SimpleNamingContextBuilder used at
 * build-time for tests/tools. Provides a very small in-memory JNDI
 * context implementation sufficient for lookups/binds used by RxFix
 * and init code. This avoids pulling the full Spring test dependency.
 */
public class SimpleNamingContextBuilder implements InitialContextFactoryBuilder, InitialContextFactory, Context {

    private final Map<String, Object> bindings = new ConcurrentHashMap<>();

    public SimpleNamingContextBuilder() {
        // constructor used via reflection
    }

    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        return this;
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return this;
    }

    // Context methods - minimal implementations
    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException {
        if (bindings.containsKey(name)) {
            return bindings.get(name);
        }
        throw new NameNotFoundException("Name not found: " + name);
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        bind(name.toString(), obj);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        bindings.put(name, obj);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        rebind(name.toString(), obj);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        bindings.put(name, obj);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        unbind(name.toString());
    }

    @Override
    public void unbind(String name) throws NamingException {
        bindings.remove(name);
    }

    @Override
    public void close() throws NamingException {
        bindings.clear();
    }

    // Remaining Context methods are unsupported for this minimal shim
    @Override public void rename(Name oldName, Name newName) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public void rename(String oldName, String newName) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public NamingEnumeration<NameClassPair> list(Name name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public NamingEnumeration<NameClassPair> list(String name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public NamingEnumeration<Binding> listBindings(Name name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public NamingEnumeration<Binding> listBindings(String name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public void destroySubcontext(Name name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public void destroySubcontext(String name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Context createSubcontext(Name name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Context createSubcontext(String name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Object lookupLink(Name name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Object lookupLink(String name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public NameParser getNameParser(Name name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public NameParser getNameParser(String name) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Name composeName(Name name, Name prefix) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public String composeName(String name, String prefix) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Object addToEnvironment(String propName, Object propVal) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Object removeFromEnvironment(String propName) throws NamingException { throw new UnsupportedOperationException(); }
    @Override public Hashtable<?, ?> getEnvironment() throws NamingException { throw new UnsupportedOperationException(); }
    @Override public String getNameInNamespace() throws NamingException { throw new UnsupportedOperationException(); }
}