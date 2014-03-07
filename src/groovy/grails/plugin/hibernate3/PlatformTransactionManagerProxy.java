package grails.plugin.hibernate3;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class PlatformTransactionManagerProxy implements PlatformTransactionManager {
    private PlatformTransactionManager targetTransactionManager;
    
    public PlatformTransactionManagerProxy() {
        
    }
    
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        return targetTransactionManager.getTransaction(definition);
    }

    public void commit(TransactionStatus status) throws TransactionException {
        targetTransactionManager.commit(status);
    }

    public void rollback(TransactionStatus status) throws TransactionException {
        targetTransactionManager.rollback(status);
    }

    public PlatformTransactionManager getTargetTransactionManager() {
        return targetTransactionManager;
    }

    public void setTargetTransactionManager(PlatformTransactionManager targetTransactionManager) {
        this.targetTransactionManager = targetTransactionManager;
    }
}
