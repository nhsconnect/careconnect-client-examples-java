package uk.nhs.careconnect.examples.App;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.hl7.fhir.instance.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.instance.model.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.nhs.careconnect.examples.fhir.*;

import javax.jms.*;

@SpringBootApplication
public class IGExplore implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(IGExplore.class, args);
	}

    IParser XMLparser = null;

    IParser JSONparser = null;

    FhirContext ctxValidator;

    FhirValidator validator;

    FhirInstanceValidator instanceValidator;

    ActiveMQConnectionFactory connectionFactory;

    Connection connection;

    Session session;

    Destination destination;

    MessageProducer producer;

    String HAPIServer = "http://fhirtest.uhn.ca/baseDstu2/";


    @Override
	public void run(String... args) throws Exception {

        if (args.length > 0 && args[0].equals("exitcode")) {
            throw new Exception();
        }
        FhirContext ctxFHIR = FhirContext.forDstu2Hl7Org();

        ctxValidator  = FhirContext.forDstu2Hl7Org();

        validator = ctxValidator.newValidator();
        instanceValidator = new FhirInstanceValidator();
        validator.registerValidatorModule(instanceValidator);

        XMLparser = ctxFHIR.newXmlParser();

        JSONparser = ctxFHIR.newJsonParser();

        FhirValidator validator = ctxFHIR.newValidator();


        connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        // Create a Connection
        connection = connectionFactory.createConnection();
        connection.start();

        // Create a Session
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Create the destination (Topic or Queue)
        destination = session.createQueue("Elastic.Queue");

        // Create a MessageProducer from the Session to the Topic or Queue
        producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);



        // This is to base HAPI server not the CareConnectAPI

        String serverBase = HAPIServer;
        //String serverBase = "http://127.0.0.1:8080/FHIRServer/DSTU2/";

        IGenericClient client = ctxFHIR.newRestfulGenericClient(serverBase);



        Organization organisation = CareConnectOrganisation.buildCareConnectOrganisation(
                "RTG",
                "Derby Teaching Hospitals NHS Foundation Trust",
                "01332 340131",
                "Uttoxeter Road",
                "",
                "Derby",
                "DE22 3NE"
                , "prov"

        );
        if (serverBase.equals(HAPIServer))
            organisation.setMeta(new Meta());

        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(organisation));
        MethodOutcome outcome = client.update().resource(organisation)
                .conditionalByUrl("Organization?identifier=" + organisation.getIdentifier().get(0).getSystem() + "%7C" + organisation.getIdentifier().get(0).getValue())
                .execute();
        organisation.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());

        AuditEvent audit = CareConnectAuditEvent.buildAuditEvent(organisation, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java");
        sendToAudit(audit);
        validate(XMLparser.encodeResourceToString(organisation));


        // GP Practice
        Organization practice = CareConnectOrganisation.buildCareConnectOrganisation(
                "C81010",
                "The Moir Medical Centre",
                "0115 9737320",
                "Regent Street",
                "Long Eaton",
                "Nottingham",
                "NG10 1QQ"
                , "prov"
        );
        if (serverBase.equals(HAPIServer))
            practice.setMeta(new Meta());

        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(practice));
        outcome = client.update().resource(practice)
                .conditionalByUrl("Organization?identifier="+practice.getIdentifier().get(0).getSystem()+"%7C"+practice.getIdentifier().get(0).getValue())
                .execute();
        practice.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());
        sendToAudit(CareConnectAuditEvent.buildAuditEvent(practice, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java"));
        validate(XMLparser.encodeResourceToString(practice));

        Location loc = new Location();
        loc.setMeta(new Meta().addProfile("https://fhir.hl7.org.uk/StructureDefinition/CareConnect-Location-1"));
        loc.setAddress(new Address().setUse(Address.AddressUse.WORK).addLine("Midland Street").addLine("Long Eaton").setCity("Nottingham").setCountry("GBR").setPostalCode("NG10 1RY"));

        loc.addIdentifier()
                .setSystem("https://fhir.nhs.uk/Id/ods-site-code")
                .setValue("RTG08")
                .setUse(Identifier.IdentifierUse.OFFICIAL);
        loc.setName("Long Eaton Clinic");

        //loc.addContained(org);
        loc.setManagingOrganization(new Reference(organisation.getId()));


        if (serverBase.equals(HAPIServer)) {
            //org.setMeta(new Meta());
            loc.setMeta(new Meta());
        }

        outcome = client.update().resource(loc)
                .conditionalByUrl("Location?identifier="+loc.getIdentifier().get(0).getSystem()+"%7C"+loc.getIdentifier().get(0).getValue())
                .execute();
        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(loc));
        audit = CareConnectAuditEvent.buildAuditEvent(loc, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java");
        sendToAudit(audit);
        validate(XMLparser.encodeResourceToString(loc));

        /*
        HealthcareService hcs = new HealthcareService();
        loc.setId("#loc");
        hcs.setLocation(new Reference(loc.getId()));
        hcs.addContained(loc);
        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(hcs));
        */


        Practitioner gp = CareConnectPractitioner.buildCareConnectPractitioner(
                "G8133438",
                "Bhatia",
                "AA",
                "Dr.",
                Enumerations.AdministrativeGender.MALE,
                "0115 9737320",
                "Regent Street",
                "Long Eaton",
                "Nottingham",
                "NG10 1QQ",
                practice,
                "R0260",
                "General Medical Practitioner"
        );
        if (serverBase.equals(HAPIServer))
            gp.setMeta(new Meta());
        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(gp));
        outcome = client.update().resource(gp)
                .conditionalByUrl("Practitioner?identifier="+gp.getIdentifier().get(0).getSystem()+"%7C"+gp.getIdentifier().get(0).getValue())
                .execute();
        gp.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());
        sendToAudit(CareConnectAuditEvent.buildAuditEvent(gp, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java"));
        validate(XMLparser.encodeResourceToString(gp));


        Practitioner gp2 = CareConnectPractitioner.buildCareConnectPractitioner(
                "G8650149",
                "Swamp",
                "Karren",
                "Dr.",
                Enumerations.AdministrativeGender.MALE,
                "0115 9737320",
                "Regent Street",
                "Long Eaton",
                "Nottingham",
                "NG10 1QQ",
                practice,
                "R0260",
                "General Medical Practitioner"
        );
        if (serverBase.equals(HAPIServer))
            gp2.setMeta(new Meta());

        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(gp2));
        outcome = client.update().resource(gp2)
                .conditionalByUrl("Practitioner?identifier="+gp2.getIdentifier().get(0).getSystem()+"%7C"+gp2.getIdentifier().get(0).getValue())
                .execute();
        gp2.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());
        sendToAudit(CareConnectAuditEvent.buildAuditEvent(gp2, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java"));
        validate(XMLparser.encodeResourceToString(gp2));

        Patient patient = CareConnectPatient.buildCareConnectPatientCSV("British - Mixed British,01,9876543210,Number present and verified,01,Kanfeld,Bernie,Miss,10 Field Jardin,Long Eaton,Nottingham,NG10 1ZZ,1,1998-03-19"
                ,practice
                ,gp );
        if (serverBase.equals(HAPIServer))
            patient.setMeta(new Meta());

		System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(patient));
		outcome = client.update().resource(patient)
                            .conditionalByUrl("Patient?identifier="+patient.getIdentifier().get(0).getSystem()+"%7C"+patient.getIdentifier().get(0).getValue())
                            .execute();
        patient.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());
        sendToAudit(CareConnectAuditEvent.buildAuditEvent(patient, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java"));
        validate(XMLparser.encodeResourceToString(patient));

        MedicationOrder prescription = CareConnectMedicationOrder.buildCareConnectMedicationOrder(patient,gp);
        if (serverBase.equals(HAPIServer))
            prescription.setMeta(new Meta());

        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(prescription));
        outcome = client.update().resource(prescription)
                .conditionalByUrl("MedicationOrder?identifier="+prescription.getIdentifier().get(0).getSystem()+"%7C"+prescription.getIdentifier().get(0).getValue())
                .execute();
        prescription.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());
        sendToAudit(CareConnectAuditEvent.buildAuditEvent(prescription, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java"));
        validate(XMLparser.encodeResourceToString(prescription));

        MedicationStatement medicationSummary = CareConnectMedicationStatement.buildCareConnectMedicationStatement(patient,gp2);
        if (serverBase.equals(HAPIServer))
            medicationSummary.setMeta(new Meta());
        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(medicationSummary));
        outcome = client.update().resource(medicationSummary)
                .conditionalByUrl("MedicationStatement?identifier="+medicationSummary.getIdentifier().get(0).getSystem()+"%7C"+medicationSummary.getIdentifier().get(0).getValue())
                .execute();
        medicationSummary.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());
        sendToAudit(CareConnectAuditEvent.buildAuditEvent(medicationSummary, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java"));
        validate(XMLparser.encodeResourceToString(medicationSummary));


        Immunization
                immunisation = CareConnectImmunization.buildCareConnectImmunization(patient, gp);
        if (serverBase.equals(HAPIServer))
            immunisation.setMeta(new Meta());
        System.out.println(XMLparser.setPrettyPrint(true).encodeResourceToString(immunisation));
        outcome = client.update().resource(immunisation)
                .conditionalByUrl("Immunization?identifier="+immunisation.getIdentifier().get(0).getSystem()+"%7C"+immunisation.getIdentifier().get(0).getValue())
                .execute();
        immunisation.setId(outcome.getId());
        System.out.println(outcome.getId().getValue());
        sendToAudit(CareConnectAuditEvent.buildAuditEvent(immunisation, outcome, "rest", "create", AuditEvent.AuditEventAction.C,"IGExplore.java"));
        validate(XMLparser.encodeResourceToString(immunisation));


        // Clean up ActiveMQ
        session.close();
        connection.close();


    }

    private void validate(String resource)
    {
        ValidationResult result = validator.validateWithResult(resource);

       // System.out.println(result.isSuccessful()); // false

        // Show the issues
        for (SingleValidationMessage next : result.getMessages()) {
            switch (next.getSeverity())
            {
                case ERROR:
                    System.out.println(" Next issue " + (char)27 + "[31mERROR" + (char)27 + "[0m" + " - " +  next.getLocationString() + " - " + next.getMessage());
                    break;
                case WARNING:
                    System.out.println(" Next issue " + (char)27 + "[33mWARNING" + (char)27 + "[0m" + " - " +  next.getLocationString() + " - " + next.getMessage());
                    break;
                case INFORMATION:
                    System.out.println(" Next issue " + (char)27 + "[34mINFORMATION" + (char)27 + "[0m" + " - " +  next.getLocationString() + " - " + next.getMessage());
                    break;
                default:
                    System.out.println(" Next issue " + next.getSeverity() + " - " + next.getLocationString() + " - " + next.getMessage());
            }
        }
    }


    private void sendToAudit(AuditEvent audit) {
        try {
            // Create a ConnectionFactory

            // Create a messages

            String text =JSONparser.setPrettyPrint(true).encodeResourceToString(audit);
            TextMessage message = session.createTextMessage(text);

            // Tell the producer to send the message
            System.out.println("Sent message: "+ message.hashCode() + " : " + Thread.currentThread().getName());
            producer.send(message);



        }
        catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}


