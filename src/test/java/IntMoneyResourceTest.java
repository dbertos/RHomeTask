import org.glassfish.grizzly.http.server.HttpServer;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rservice.JerseyMain;
import rservice.entities.Account;
import rservice.utils.HibernateUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class IntMoneyResourceTest {

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        server = JerseyMain.startServer();
        Client client = ClientBuilder.newClient();
        target = client.target(JerseyMain.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testGetBalance() {
        int accountId = 4;
        String path = "money/balance/" + accountId;
        String responseMessage = target.path(path).request().get(String.class);

        assertEquals("Account with id 4 has balance 40000000", responseMessage);
    }

    @Test
    public void testTransferFailsWhenIllegalAmount() {
        int senderId = 1;
        int receiverId = 2;
        BigDecimal illegalAmount = new BigDecimal(-100);

        String responseMessage = makePostRequestToTransferMoney(senderId, receiverId, illegalAmount);

        assertEquals("Incorrect amount", responseMessage);
    }

    @Test
    public void testTransferFailsWhenSendingToYourself() {
        int senderId = 1;
        int receiverId = senderId;
        BigDecimal amount = new BigDecimal(1000);

        String responseMessage = makePostRequestToTransferMoney(senderId, receiverId, amount);

        assertEquals("Cannot transfer funds to yourself", responseMessage);
    }

    @Test
    public void testTransferFailsWhenInsufficientFunds() {
        int senderId = 1;
        int receiverId = 2;
        BigDecimal amount = new BigDecimal(1000000000);

        String responseMessage = makePostRequestToTransferMoney(senderId, receiverId, amount);

        assertEquals("Insufficient funds", responseMessage);
    }

    @Test
    public void testTransferMoney() {
        int senderId = 1;
        int receiverId = 2;
        BigDecimal amount = new BigDecimal(1000);

        BigDecimal currentBalance = getAccountBalance(receiverId);

        String responseMessage = makePostRequestToTransferMoney(senderId, receiverId, amount);

        assertEquals("Transfer from account with id 1 to account with id 2 succeeded", responseMessage);

        BigDecimal expectedBalance = currentBalance.add(amount);
        BigDecimal updatedBalance = getAccountBalance(receiverId);
        assertEquals(expectedBalance, updatedBalance);
    }

    @Test
    public void testMakeTransfersConcurrently() throws InterruptedException {
        int firstAccountId = 1;
        int secondAccountId = 2;
        BigDecimal amount = new BigDecimal(1000);

        BigDecimal firstAccountBalanceBeforeTransfers = getAccountBalance(firstAccountId);
        BigDecimal secondAccountBalanceBeforeTransfers = getAccountBalance(secondAccountId);

        Thread transferForth = createThreadToTransferMoneyHundredTimes(firstAccountId, secondAccountId, amount);
        Thread transferBack = createThreadToTransferMoneyHundredTimes(secondAccountId, firstAccountId, amount);

        transferForth.start();
        transferBack.start();

        transferForth.join();
        transferBack.join();

        BigDecimal firstAccountBalanceAfterTransfers = getAccountBalance(firstAccountId);
        BigDecimal secondAccountBalanceAfterTransfers = getAccountBalance(secondAccountId);

        assertEquals(firstAccountBalanceBeforeTransfers, firstAccountBalanceAfterTransfers);
        assertEquals(secondAccountBalanceBeforeTransfers, secondAccountBalanceAfterTransfers);
    }

    private Thread createThreadToTransferMoneyHundredTimes(final int senderId, final int receiverId, final BigDecimal amount) {
        return new Thread() {
            public void run() {
                for (int i = 0; i < 100; i++) {
                    makePostRequestToTransferMoney(senderId, receiverId, amount);
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private BigDecimal getAccountBalance(int accountId) {
        final Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            Account account = (Account) session.get(Account.class, accountId);

            return account.getBalance();
        } finally {
            session.close();
        }
    }

    private String makePostRequestToTransferMoney(int senderId, int receiverId, BigDecimal amount) {
        String path = "money/transfer/" + senderId + "/" + receiverId + "/" + amount;
        return target.path(path).request().post(Entity.json(null), String.class);
    }
}
