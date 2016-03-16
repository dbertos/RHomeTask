package rservice.services;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import rservice.entities.Account;
import rservice.utils.HibernateUtil;

import java.math.BigDecimal;

public class MoneyService {

    public BigDecimal getBalance(int accountId) {
        final Session session = getSession();

        try {
            Account account = (Account) session.get(Account.class, accountId);

            validateAccount(account);

            return account.getBalance();
        } finally {
            session.close();
        }
    }

    private void validateAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account does not exist");
        }
    }

    public synchronized void transferMoney(int senderId, int receiverId, BigDecimal amount) {
        final Session session = getSession();
        try {
            validateAmount(amount);
            checkSenderDifferentToReceiver(senderId, receiverId);

            Account sender = (Account) session.get(Account.class, senderId);
            validateSender(sender);

            Account receiver = (Account) session.get(Account.class, receiverId);
            validateReceiver(receiver);

            validateThatSenderHasEnoughFunds(amount, sender);
            makeActualMoneySwap(amount, session, sender, receiver);
        } finally {
            session.close();
        }
    }

    private void makeActualMoneySwap(BigDecimal amount, Session session, Account sender, Account receiver) {
        Transaction transaction = session.beginTransaction();

        sender.withdraw(amount);
        receiver.deposit(amount);

        session.saveOrUpdate(sender);
        session.saveOrUpdate(receiver);

        transaction.commit();
    }

    private void validateThatSenderHasEnoughFunds(BigDecimal amount, Account sender) {
        if (!sender.hasSufficientBalance(amount)) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    private void validateReceiver(Account receiver) {
        if (receiver == null) {
            throw new IllegalArgumentException("Receiver does not exist");
        }
    }

    private void validateSender(Account sender) {
        if (sender == null) {
            throw new IllegalArgumentException("Sender does not exist");
        }
    }

    private void checkSenderDifferentToReceiver(int senderId, int receiverId) {
        if (senderId == receiverId) {
            throw new IllegalArgumentException("Cannot transfer funds to yourself");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Incorrect amount");
        }
    }

    private Session getSession() throws HibernateException {
        return HibernateUtil.getSessionFactory().openSession();
    }
}