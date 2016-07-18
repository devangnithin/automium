package com.nithind.automium;

import com.nithind.automium.exceptions.DuplicateIdException;
import com.nithind.automium.exceptions.PreExecutionException;
import com.nithind.automium.reporters.Reporter;
import com.nithind.automium.test.PreReqTestCase;
import com.nithind.automium.test.Test;
import com.nithind.automium.test.Tests;
import com.nithind.automium.constants.ExecutionStatus;
import com.nithind.automium.exceptions.NotAutomiumTestCaseException;
import com.nithind.automium.xmlsupport.Automium;
import com.nithind.automium.xmlsupport.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nithin Devang.
 */

public class AutomiumExecutor {

    private static List<String> executedTestCase = new ArrayList<String>();


    public void run(File xmlPath) throws Exception {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Automium.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Automium automiumXMLRoot = (Automium) jaxbUnmarshaller.unmarshal(xmlPath);
            List<Test> TestList = automiumXMLRoot.getTests().getTest();

            if(hasDuplicateId(TestList)) {
                throw new DuplicateIdException();
            }

            Reporter.produceResult();
            for (Test test : TestList) {
                /*if(test.getPreTestCases() !=  null) {
                    checkPreMandatory(test.getPreTestCases());
                }*/

                Class<?> clazz = Class.forName(test.getClazz());
                Constructor<?> ctor = clazz.getConstructor();
                Object object = ctor.newInstance(new Object[]{});

                TestCase testCase = null;
                if (object instanceof TestCase) {
                    testCase = (TestCase) object;
                } else {
                    throw new NotAutomiumTestCaseException();
                }

                executeTestCases(testCase);
                executedTestCase.add(test.getId());
            }

        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

  /*  private boolean checkPreMandatory(List<PreReqTestCase> preList) throws PreExecutionException {
        for (PreReqTestCase pre : preList) {
                if (!executedTestCase.contains(pre.getId())) {
                    throw new PreExecutionException("for Id "+pre.getId());
                }
        }
        return true;
    }*/


    private boolean hasDuplicateId(List<Test> xmlTestCaseList) {
        List<String> idList = new ArrayList<String>();
        for (Test xmlTestCase : xmlTestCaseList) {
            if (xmlTestCase.getId() != null) {
                if (idList.contains(xmlTestCase.getId())) {
                    return true;
                }
                idList.add(xmlTestCase.getId());
            }
        }
        return false;
    }

    private void executeTestCases(TestCase testCase) {
        AutomiumLog autoLog = testCase.run();

        if (autoLog.getExecutionStatus() == ExecutionStatus.SUCCESS) {
            Reporter.success(testCase.getClass().getCanonicalName(), "Yes");
        } else if (autoLog.getExecutionStatus() == ExecutionStatus.FAIL) {
            Reporter.fail(testCase.getClass().getCanonicalName(), "");
        } else if (autoLog.getExecutionStatus() == ExecutionStatus.WARN) {
            Reporter.warn(testCase.getClass().getCanonicalName(), "");
        }
        if (autoLog.getExecutionStatus() == ExecutionStatus.NOLOGS) {
            Reporter.warn(testCase.getClass().getCanonicalName(), "No Log Reports Found In The Execution Returned");
        }
        if (autoLog.getExecutionStatus() != ExecutionStatus.NOLOGS) {

            Reporter.setClassLogs(autoLog);
        }
    }
}