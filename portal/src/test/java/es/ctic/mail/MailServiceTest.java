package es.ctic.mail;

import es.inteco.common.Constants;
import es.inteco.rastreador2.actionform.basic.service.BasicServiceForm;
import es.inteco.utils.FileUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by mikunis on 1/25/17.
 */
public class MailServiceTest {

//    @Test
//    public void sendBasicServiceErrorMessageTest() {
//        final MailService mailService = new MailService();
//        final BasicServiceForm basicServiceForm = new BasicServiceForm();
//        basicServiceForm.setEmail("miguel.garcia5@externos.seap.minhap.es");
//        mailService.sendBasicServiceErrorMessage(basicServiceForm, "Prueba de envío desde MailServiceTest.sendBasicServiceErrorMessageTest");
//    }
//
//    @Test
//    public void sendBasicServiceReportTest() {
//        final MailService mailService = new MailService();
//        final BasicServiceForm basicServiceForm = new BasicServiceForm();
//        basicServiceForm.setEmail("miguel.garcia5@externos.seap.minhap.es");
//        basicServiceForm.setReport(Constants.REPORT_OBSERVATORY_2_NOBROKEN);
//        basicServiceForm.setUser("Junit");
//        basicServiceForm.setDomain("Test");
//        basicServiceForm.setProfundidad("1");
//        basicServiceForm.setAmplitud("1");
//        basicServiceForm.setInDirectory(false);
//
//        mailService.sendBasicServiceReport(basicServiceForm, "/home/mikunis/Downloads/attachment_prueba.zip", "attachment_mailservicetest_sendreport.zip");
//    }

}