package com.global.api.tests.bill_pay;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.Customer;
import com.global.api.entities.HostedPaymentData;
import com.global.api.entities.Transaction;
import com.global.api.entities.billing.Bill;
import com.global.api.entities.billing.LoadHostedPaymentResponse;
import com.global.api.entities.billing.LoadSecurePayResponse;
import com.global.api.entities.enums.AccountType;
import com.global.api.entities.enums.BillPresentment;
import com.global.api.entities.enums.CheckType;
import com.global.api.entities.enums.HostedPaymentType;
import com.global.api.entities.enums.SecCode;
import com.global.api.entities.enums.ServiceEndpoints;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.ConfigurationException;
import com.global.api.entities.exceptions.GatewayException;
import com.global.api.entities.exceptions.ValidationException;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.paymentMethods.RecurringPaymentMethod;
import com.global.api.paymentMethods.eCheck;
import com.global.api.serviceConfigs.BillPayConfig;
import com.global.api.services.BillPayService;
import com.global.api.utils.StringUtils;

public class BillPayTests {
    eCheck ach;
    CreditCardData clearTextCredit;
    Address address;
    Customer customer;
    Bill bill;
    List<Bill> bills;
    Bill billLoad;
    Bill blindBill;

    public BillPayTests() throws ConfigurationException {
        BillPayConfig config = new BillPayConfig();
        config.setMerchantName("IntegrationTesting");
        config.setUsername("IntegrationTestCashier");
        config.setPassword("G?vaXhg6<@V'Y)-m");
        config.setServiceUrl(ServiceEndpoints.BILLPAY_CERTIFICATION);
        config.setEnableLogging(true);

        ServicesContainer.configureService(config);

        config = new BillPayConfig();
        config.setMerchantName("IntegrationTestingBillUpload");
        config.setUsername("IntegrationTestCashier");
        config.setPassword("G?vaXhg6<@V'Y)-m");
        config.setServiceUrl(ServiceEndpoints.BILLPAY_CERTIFICATION);
        config.setEnableLogging(true);
        ServicesContainer.configureService(config, "billload");

        ach = new eCheck();
        ach.setAccountNumber("12345");
        ach.setRoutingNumber("064000017");
        ach.setAccountType(AccountType.Checking);
        ach.setCheckType(CheckType.Business);
        ach.setSecCode(SecCode.Web);
        ach.setCheckHolderName("Tester");
        ach.setBankName("Regions");

        clearTextCredit = new CreditCardData();
        clearTextCredit.setNumber("4444444444444448");
        clearTextCredit.setExpMonth(12);
        clearTextCredit.setExpYear(2025);
        clearTextCredit.setCvn("123");
        clearTextCredit.setCardHolderName("Test Tester");

        address = new Address();
        address.setStreetAddress1("1234 Test St");
        address.setStreetAddress2("Apt 201");
        address.setCity("Auburn");
        address.setState("AL");
        address.setCountry("US");
        address.setPostalCode("12345");

        customer = new Customer();
        customer.setAddress(address);
        customer.setEmail("testemailaddress@e-hps.com");
        customer.setFirstName("Test");
        customer.setLastName("Tester");
        customer.setHomePhone("555-555-4444");

        bill = new Bill();
        bill.setAmount(new BigDecimal(50));
        bill.setIdentifier1("12345");

        Bill bill1 = new Bill();
        bill1.setBillType("Tax Payments");
        bill1.setIdentifier1("123");
        bill1.setAmount(new BigDecimal(10));
        Bill bill2 = new Bill();
        bill2.setBillType("Tax Payments");
        bill2.setIdentifier1("321");
        bill2.setAmount(new BigDecimal(10));
        bills = new ArrayList<>();
        bills.add(bill1);
        bills.add(bill2);

        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH) + 3);

        billLoad = new Bill();
        billLoad.setAmount(new BigDecimal(50));
        billLoad.setBillType("Tax Payments");
        billLoad.setIdentifier1("12345");
        billLoad.setIdentifier2("23456");
        billLoad.setBillPresentment(BillPresentment.FULL);
        billLoad.setDueDate(now.getTime());
        billLoad.setCustomer(customer);

        now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH) + 1);

        blindBill = new Bill();
        blindBill.setAmount(new BigDecimal(50));
        blindBill.setBillType("Tax Payments");
        blindBill.setIdentifier1("12345");
        blindBill.setIdentifier2("23456");
        blindBill.setBillPresentment(BillPresentment.FULL);
        blindBill.setDueDate(now.getTime());
        blindBill.setCustomer(customer);
    }

    // #region Authorization Builder Cases

    @Test
    public void Charge_WithSingleBill_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        BigDecimal fee = service.calculateConvenienceAmount(clearTextCredit, bill.getAmount());

        Transaction result = clearTextCredit.charge(bill.getAmount())
            .withAddress(address)
            .withBills(bill)
            .withConvenienceAmt(fee)
            .withCurrency("USD")
            .execute();

        validateSuccesfulTransaction(result);
    }

    @Test
    public void Charge_WithMultipleBills_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        BigDecimal totalAmount = new BigDecimal(0);
        
        for (Bill bill : bills) {
            totalAmount = totalAmount.add(bill.getAmount());
        }
        
        BigDecimal fee = service.calculateConvenienceAmount(clearTextCredit, totalAmount);

        Transaction result = clearTextCredit
            .charge(totalAmount)
            .withAddress(address)
            .withBills(bills)
            .withConvenienceAmt(fee)
            .withCurrency("USD")
            .execute();

        validateSuccesfulTransaction(result);
    }

    @Test
    public void Tokenize_UsingCreditCard_ReturnsToken() throws ApiException {
        Address address = new Address();
        address.setPostalCode("12345");

        Transaction result = clearTextCredit.verify()
            .withAddress(address)
            .withRequestMultiUseToken(true)
            .execute();

        assertFalse(StringUtils.isNullOrEmpty(result.getToken()));
    }

    @Test
    public void UpdateTokenExpiry_UsingCreditCardToken_DoesNotThrow() throws ApiException {
        Address address = new Address();
        address.setPostalCode("12345");

        Transaction result = clearTextCredit.verify()
            .withAddress(address)
            .withRequestMultiUseToken(true)
            .execute();

        assertFalse(StringUtils.isNullOrEmpty(result.getToken()));

        try {
            clearTextCredit.setToken(result.getToken());
            clearTextCredit.setExpMonth(12);
            clearTextCredit.setExpYear(2022);

            clearTextCredit.updateTokenExpiry();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void Tokenize_UsingACH_ReturnsToken() throws ApiException {
        Address address = new Address();
        address.setPostalCode("12345");

        Transaction result = ach.verify()
            .withAddress(address)
            .execute();

        assertFalse(StringUtils.isNullOrEmpty(result.getToken()));
    }

    @Test
    public void Charge_UsingTokenizedCreditCard_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        Address address = new Address();
        address.setPostalCode("12345");

        Transaction result = clearTextCredit.verify()
            .withAddress(address)
            .withRequestMultiUseToken(true)
            .execute();

        String token = result.getToken();
        assertFalse(StringUtils.isNullOrEmpty(token));

        BigDecimal fee = service.calculateConvenienceAmount(clearTextCredit, bill.getAmount());

        CreditCardData paymentMethod = new CreditCardData();
        paymentMethod.setToken(token);
        paymentMethod.setExpMonth(clearTextCredit.getExpMonth());
        paymentMethod.setExpYear(clearTextCredit.getExpYear());

        assertFalse(StringUtils.isNullOrEmpty(token));

        result = paymentMethod
            .charge(bill.getAmount())
            .withAddress(address)
            .withBills(bill)
            .withConvenienceAmt(fee)
            .withCurrency("USD")
            .execute();

        validateSuccesfulTransaction(result);
    }

    @Test
    public void Charge_UsingTokenizedACH_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        Address address = new Address();
        address.setPostalCode("12345");

        Transaction result = ach.verify()
            .withAddress(address)
            .execute();

        String token = result.getToken();
        assertFalse(StringUtils.isNullOrEmpty(token));
        BigDecimal fee = service.calculateConvenienceAmount(ach, bill.getAmount());

        eCheck paymentMethod = new eCheck();
        paymentMethod.setAccountType(AccountType.Checking);
        paymentMethod.setCheckType(CheckType.Business);
        paymentMethod.setSecCode(SecCode.Web);
        paymentMethod.setCheckHolderName("Tester");
        paymentMethod.setToken(token);

        assertFalse(StringUtils.isNullOrEmpty(token));

        result = paymentMethod
                .charge(bill.getAmount())
                .withBills(bill)
                .withConvenienceAmt(fee)
                .withCurrency("USD")
                .withAddress(address)
                .execute();

        validateSuccesfulTransaction(result);
    }

    @Test
    public void Charge_UsingTokenFromPreviousPayment_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        BigDecimal fee = service.calculateConvenienceAmount(clearTextCredit, bill.getAmount());

        Transaction transaction = clearTextCredit
            .charge(bill.getAmount())
            .withAddress(address)
            .withBills(bill)
            .withConvenienceAmt(fee)
            .withCurrency("USD")
            .withRequestMultiUseToken(true)
            .execute();

        validateSuccesfulTransaction(transaction);
        assertFalse(StringUtils.isNullOrEmpty(transaction.getToken()));

        CreditCardData token = new CreditCardData();
        token.setToken(transaction.getToken());
        token.setExpYear(clearTextCredit.getExpYear());
        token.setExpMonth(clearTextCredit.getExpMonth());

        try {
        Transaction result = token.charge(bill.getAmount())
            .withBills(bill)
            .withConvenienceAmt(fee)
            .withCurrency("USD")
            .execute();
        } catch (ValidationException ex) {
            System.out.println(ex);
        }

        // validateSuccesfulTransaction(result);
    }

    @Test(expected = ValidationException.class)
    public void Charge_WithoutAddingBills_ThrowsValidationException() throws ApiException {
        clearTextCredit
            .charge(new BigDecimal(50))
            .withCurrency("USD")
            .withConvenienceAmt(new BigDecimal(3))
            .execute();
    }

    @Test(expected = ValidationException.class)
    public void Charge_WithMismatchingAmounts_ThrowsValidationException() throws ApiException {
        clearTextCredit
            .charge(new BigDecimal(60))
            .withBills(bills)
            .withCurrency("USD")
            .execute();
    }

    // #endregion

    // #region Management Builder Cases

    @Test
    public void ReversePayment_WithPreviousTransaction_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        BigDecimal fee = service.calculateConvenienceAmount(clearTextCredit, bill.getAmount());

        // Make transaction to reverse
        Transaction transaction = clearTextCredit
            .charge(bill.getAmount())
            .withAddress(address)
            .withBills(bill)
            .withConvenienceAmt(fee)
            .withCurrency("USD")
            .execute();

        validateSuccesfulTransaction(transaction);

        // Now reverse it
        Transaction reversal = Transaction.fromId(transaction.getTransactionId())
            .reverse(bill.getAmount())
            .withConvenienceAmt(fee)
            .execute();

        validateSuccesfulTransaction(reversal);
    }

    @Test
    public void ReversePayment_WithPreviousMultiBillTransaction_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        BigDecimal totalAmount = new BigDecimal(0);
        
        for (Bill bill : bills) {
            totalAmount = totalAmount.add(bill.getAmount());
        }
        
        BigDecimal fee = service.calculateConvenienceAmount(clearTextCredit, totalAmount);

        // Make transaction to reverse
        Transaction transaction = clearTextCredit
            .charge(totalAmount)
            .withAddress(address)
            .withBills(bills)
            .withConvenienceAmt(fee)
            .withCurrency("USD")
            .execute();

        validateSuccesfulTransaction(transaction);

        // Now reverse it
        Transaction reversal = Transaction.fromId(transaction.getTransactionId())
            .reverse(totalAmount)
            .withConvenienceAmt(fee)
            .execute();

        validateSuccesfulTransaction(reversal);
    }

    @Test
    public void PartialReversal_WithCreditCard_ReturnsSuccessfulTransaction() throws ApiException {
        BillPayService service = new BillPayService();
        BigDecimal totalAmount = new BigDecimal(0);
        
        for (Bill bill : bills) {
            totalAmount = totalAmount.add(bill.getAmount());
        }
        
        BigDecimal fee = service.calculateConvenienceAmount(clearTextCredit, totalAmount);

        // Make transaction to reverse
        Transaction transaction =  clearTextCredit
                .charge(totalAmount)
                .withAddress(address)
                .withBills(bills)
                .withPaymentMethod(clearTextCredit)
                .withConvenienceAmt(fee)
                .withCurrency("USD")
                .execute();

        validateSuccesfulTransaction(transaction);

        // Now reverse it
        List<Bill> billsToPariallyReverse = new ArrayList<>(); 
        for (Bill x : bills) {
            Bill bill = new Bill();
            bill.setBillType(x.getBillType());
            bill.setIdentifier1(x.getIdentifier1());
            bill.setAmount(x.getAmount().subtract(new BigDecimal(5)));

            billsToPariallyReverse.add(bill);
        }

        BigDecimal newTotalAmount = totalAmount.subtract(new BigDecimal(10));
        BigDecimal newFees = service.calculateConvenienceAmount(clearTextCredit, newTotalAmount);

        Transaction reversal = Transaction.fromId(transaction.getTransactionId())
            .reverse(newTotalAmount)
            .withBills(billsToPariallyReverse)
            .withConvenienceAmt(fee.subtract(newFees))
            .execute();

        validateSuccesfulTransaction(reversal);
    }

    // #endregion

    // #region Billing Builder Cases

    @Test
    public void LoadHostedPayment_WithMakePaymentType_ReturnsIdentifier() throws ApiException {
        BillPayService service = new BillPayService();
        HostedPaymentData data = new HostedPaymentData();
        
        List<Bill> bills = new ArrayList<>();
        bills.add(blindBill);

        Address address = new Address();
        address.setStreetAddress1("123 Drive");
        address.setPostalCode("12345");

        data.setBills(bills);
        data.setCustomerAddress(address);
        data.setCustomerEmail("test@tester.com");
        data.setCustomerFirstName("Test");
        data.setCustomerLastName("Tester");
        data.setHostedPaymentType(HostedPaymentType.MAKE_PAYMENT);

        LoadSecurePayResponse response = service.loadHostedPayment(data);

        assertTrue(!StringUtils.isNullOrEmpty(response.getPaymentIdentifier()));

    }

    @Test
    public void LoadHostedPayment_WithMakePaymentReturnToken_ReturnsIdentifier() throws ApiException {
        BillPayService service = new BillPayService();
        HostedPaymentData hostedPaymentData = new HostedPaymentData();
        
        List<Bill> bills = new ArrayList<>();
        bills.add(blindBill);

        Address address = new Address();
        address.setStreetAddress1("123 Drive");
        address.setCity("Auburn");
        address.setState("AL");
        address.setPostalCode("36830");
        address.setCountryCode("US");

        hostedPaymentData.setBills(bills);
        hostedPaymentData.setCustomerAddress(address);
        hostedPaymentData.setCustomerEmail("test@tester.com");
        hostedPaymentData.setCustomerFirstName("Test");
        hostedPaymentData.setCustomerLastName("Tester");
        hostedPaymentData.setCustomerPhoneMobile("800-555-5555");
        hostedPaymentData.setCustomerIsEditable(true);
        hostedPaymentData.setHostedPaymentType(HostedPaymentType.MAKE_PAYMENT_RETURN_TOKEN);

        LoadHostedPaymentResponse response = service.loadHostedPayment(hostedPaymentData);

        assertTrue(!StringUtils.isNullOrEmpty(response.getPaymentIdentifier()));
    }

    @Test(expected = ValidationException.class)
    public void LoadHostedPayment_WithoutBills_ThrowsValidationException() throws ApiException {
        BillPayService service = new BillPayService();
        HostedPaymentData hostedPaymentData = new HostedPaymentData();
        
        Address address = new Address();
        address.setStreetAddress1("123 Drive");

        hostedPaymentData.setCustomerAddress(address);
        hostedPaymentData.setCustomerEmail("alexander.molbert@e-hps.com");
        hostedPaymentData.setCustomerFirstName("Alex");
        hostedPaymentData.setHostedPaymentType(HostedPaymentType.MAKE_PAYMENT);

        LoadHostedPaymentResponse response = service.loadHostedPayment(hostedPaymentData);
    }

    @Test(expected = ValidationException.class)
    public void LoadHostedPayment_WithoutPaymentType_ThrowsValidationException() throws ApiException {
        BillPayService service = new BillPayService();
        HostedPaymentData hostedPaymentData = new HostedPaymentData();
        
        List<Bill> bills = new ArrayList<>();
        bills.add(blindBill);
        
        Address address = new Address();
        address.setStreetAddress1("123 Drive");

        hostedPaymentData.setBills(bills);
        hostedPaymentData.setCustomerAddress(address);
        hostedPaymentData.setCustomerEmail("alexander.molbert@e-hps.com");
        hostedPaymentData.setCustomerFirstName("Alex");

        service.loadHostedPayment(hostedPaymentData);
    }

    @Test
    public void Load_WithOneBill_DoesNotThrow() {
        try {
            BillPayService service = new BillPayService();
        
            List<Bill> bills = new ArrayList<>();
            bills.add(blindBill);

            service.loadBills(bills, "billload");
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void Load_WithOneThousandBills_DoesNotThrow() {
        try {
            BillPayService service = new BillPayService();

            service.loadBills(makeNumberOfBills(1000), "billload");
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void Load_WithFiveThousandBills_DoesNotThrow() {
        try {
            BillPayService service = new BillPayService();

            service.loadBills(makeNumberOfBills(5000), "billload");
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test(expected = GatewayException.class)
    public void Load_WithDuplicateBills_ThrowsGatewayException() throws ApiException {
        BillPayService service = new BillPayService();
        List<Bill> bills = new ArrayList<>();
        bills.add(billLoad);
        bills.add(billLoad);

        service.loadBills(bills, "billload");
    }

    @Test(expected = GatewayException.class)
    public void Load_WithInvalidBillType_ThrowsGatewayException() throws ApiException {
        BillPayService service = new BillPayService();
        List<Bill> bills = new ArrayList<>();
        bills.add(billLoad);
        
        Bill newBill = new Bill();
        newBill.setAmount(billLoad.getAmount());
        newBill.setBillPresentment(billLoad.getBillPresentment());
        newBill.setBillType("InvalidBillType");
        newBill.setCustomer(billLoad.getCustomer());
        newBill.setDueDate(billLoad.getDueDate());
        newBill.setIdentifier1(billLoad.getIdentifier1());
        bills.add(newBill);

        service.loadBills(bills, "billload");
    }

    // #endregion

    // #region Recurring Builder Cases

    @Test
    public void Create_Customer_ReturnsCustomer() {
        try {
            customer = new Customer();
            customer.setFirstName("IntegrationCreate");
            customer.setLastName("Customer");
            customer.setEmail("test.test@test.com");
            customer.setId(UUID.randomUUID().toString());
            customer = customer.create();

            assertEquals("IntegrationCreate", customer.getFirstName());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void Update_Customer_ReturnsCustomer() {
        try {
            customer = new Customer();
            customer.setFirstName("IntegrationUpdate");
            customer.setLastName("Customer");
            customer.setEmail("test.test@test.com");
            customer.setId(UUID.randomUUID().toString());
            customer = customer.create();

            assertEquals("IntegrationUpdate", customer.getFirstName());

            customer.setFirstName("Updated");

            customer.saveChanges();

            assertEquals("Updated", customer.getFirstName());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void Delete_Customer_ReturnsCustomer() {
        String id = UUID.randomUUID().toString();

        try {
            customer = new Customer();
            customer.setFirstName("IntegrationDelete");
            customer.setLastName("Customer");
            customer.setEmail("test.test@test.com");
            customer.setId(id);
            customer = customer.create();

            assertEquals("IntegrationDelete", customer.getFirstName());

            customer.delete();

            // Bill pay currently does not support retrieval of customer, so there is no true
            // way to validate the customer was deleted other than no exception was thrown
            assertEquals("IntegrationDelete", customer.getFirstName());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void Create_CustomerAccount_ReturnsPaymentMethod() {
        try {
            customer = new Customer();
            customer.setFirstName("Integration");
            customer.setLastName("Customer");
            customer.setEmail("test.test@test.com");
            customer.setId(UUID.randomUUID().toString());
            customer = customer.create();

            RecurringPaymentMethod paymentMethod = customer.addPaymentMethod(UUID.randomUUID().toString(), clearTextCredit).create();

            assertFalse(StringUtils.isNullOrEmpty(paymentMethod.getKey()));
        } catch (Exception ex) {
            fail((ex.getCause() != null ? ex.getCause() : ex).getMessage());
        }
    }

    @Test
    public void Update_CustomerAccount_ReturnsSuccess() {
        try {
            customer = new Customer();
            customer.setFirstName("Account");
            customer.setLastName("Update");
            customer.setEmail("account.update@test.com");
            customer.setId(UUID.randomUUID().toString());
            customer = customer.create();

            RecurringPaymentMethod paymentMethod = customer.addPaymentMethod(UUID.randomUUID().toString(), clearTextCredit).create();

            assertFalse(StringUtils.isNullOrEmpty(paymentMethod.getKey()));

            ((CreditCardData) paymentMethod.getPaymentMethod()).setExpYear(2026);

            paymentMethod.saveChanges();
        } catch (Exception ex) {
            fail((ex.getCause() != null ? ex.getCause() : ex).getMessage());
        }
    }

    @Test
    public void Delete_CustomerAccount_ReturnsSuccess() {
        try {
            customer = new Customer();
            customer.setFirstName("Account");
            customer.setLastName("Delete");
            customer.setEmail("account.delete@test.com");
            customer.setId(UUID.randomUUID().toString());
            customer = customer.create();

            RecurringPaymentMethod paymentMethod = customer.addPaymentMethod(UUID.randomUUID().toString(), clearTextCredit).create();

            assertFalse(StringUtils.isNullOrEmpty(paymentMethod.getKey()));

            paymentMethod.delete();
        } catch (Exception ex) {
            fail((ex.getCause() != null ? ex.getCause() : ex).getMessage());
        }
    }

    @Test(expected = ApiException.class)
    public void Delete_NonexistingCustomer_ThrowsApiException() throws ApiException {
        Customer customer = new Customer();
        customer.setFirstName("Incog");
        customer.setLastName("Anony");
        customer.setId("DoesntExist");
        customer.delete();
    }

    // #endregion

    // #region Helpers

    private void validateSuccesfulTransaction(Transaction transaction) {
        int transactionId = Integer.parseInt(transaction.getTransactionId());

        assertNotEquals(transaction.getResponseMessage(), transactionId, 0);
    }
    
    private List<Bill> makeNumberOfBills(int number) {
        List<Bill> bills = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            Bill bill = new Bill();
            bill.setAmount(billLoad.getAmount());
            bill.setBillPresentment(billLoad.getBillPresentment());
            bill.setBillType(billLoad.getBillType());
            bill.setCustomer(billLoad.getCustomer());
            bill.setDueDate(billLoad.getDueDate());
            bill.setIdentifier1(String.format("%s", i));
            bill.setIdentifier2(String.format("%s", i));
            bills.add(bill);
        }

        return bills;
    }

    // #endregion
}
