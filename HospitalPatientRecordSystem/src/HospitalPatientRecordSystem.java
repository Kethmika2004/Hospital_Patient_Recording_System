/**
 * Hospital Patient Record System
 * This program simulates a hospital patient record system based on the
 * Readers-Writers Problem. The system models the following roles:

 *   Nurses (5) – readers; multiple nurses may read simultaneously.
 *   Doctors (2)– writers; require exclusive access to update
 *                 diagnoses and prescriptions.
 *   MLT (1)    – writer; requires exclusive access to add test results.

 * The implementation uses the write preferring readers-writers
 * solution to prevent writer starvation:

 *   A {@code readLock} semaphore (1 permit) protects the shared
 *       {@code readerCount}.
 *   A {@code writeLock} semaphore (1 permit) provides mutual exclusion for
 *       writers and is held by the first reader while readers are active
 *       (blocking new writers).
 *   A {@code noWritersWaiting} semaphore (1 permit) lets arriving readers
 *       queue behind any waiting writer, preventing writer starvation.

 * Liveness guarantees:

 *   Mutual exclusion – no writer writes while another writer or any reader
 *                      is active.
 *   No deadlock – all semaphores are acquired in a consistent order.
 *   No starvation – fair semaphores and the write-preference protocol
 *       ensure both readers and writers eventually proceed.
 *
 * @ Team :- MultiThreaders
 */

import java.util.concurrent.Semaphore;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


// 01. PatientRecord
/**
 * Represents a shared patient record in the hospital database.
 * The record stores simple text content for simulation purposes. In a real
 * system this would be a full patient data object. Direct field access is
 * intentionally avoided, all concurrent access goes through {@link RecordManager}
 */
class PatientRecord {

    private final String patientName;
    private String diagnosis;
    private String labResult;

    PatientRecord(String patientName) {
        this.patientName = patientName;
        this.diagnosis   = "No diagnosis yet.";
        this.labResult   = "No lab results yet.";
    }

     // Returns the patient's name.
    String getPatientName() { return patientName; }

    // Returns the current diagnosis string.
    String getDiagnosis() { return diagnosis; }

     // Sets a new diagnosis (called exclusively by a Doctor writer).
    void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

     // Returns the latest lab result string
    String getLabResult() { return labResult; }

     // Sets a new lab result (called exclusively by an MLT writer)
    void setLabResult(String labResult) { this.labResult = labResult; }
}


// 02. RecordManager

/**
 * Manages concurrent access to a {@link PatientRecord} using a
 * write-preferring readers-writers protocol backed by semaphores.

 *  Reader enter:
 *    1. acquire noWritersWaiting  (queues behind any pending writer)
 *    2. acquire readLock          (protects readerCount)
 *    3. if first reader → acquire writeLock
 *    4. release readLock
 *    5. release noWritersWaiting
 *
 *  Reader exit:
 *    1. acquire readLock
 *    2. if last reader → release writeLock
 *    3. release readLock

 *  Writer enter:
 *    1. acquire noWritersWaiting  (blocks new readers from jumping ahead)
 *    2. acquire writeLock         (wait for readers/other writers to finish)
 *    3. release noWritersWaiting

 *  Writer exit:
 *    1. release writeLock
 */

class RecordManager {

    static final Logger LOG = Logger.getLogger("Hospital");

    static {
        Logger root = Logger.getLogger("");
        for (var h : root.getHandlers()) root.removeHandler(h);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord r) {
                return String.format("[%-6s] %s%n",
                        r.getLevel().getLocalizedName(), r.getMessage());
            }
        });
        LOG.addHandler(ch);
        LOG.setUseParentHandlers(false);
    }

    private final PatientRecord record;

    private int readerCount = 0;

    private final Semaphore readLock = new Semaphore(1, true);

    private final Semaphore writeLock = new Semaphore(1, true);


//     Prevents reader starvation of writers: new readers must pass through
//     this gate, which a writer holds while waiting for {@code writeLock},
//     forcing readers to queue behind the writer.

    private final Semaphore noWritersWaiting = new Semaphore(1, true);


     //Constructs a RecordManager wrapping the given patient record.
    RecordManager(PatientRecord record) {
        this.record = record;
    }

    // ── Reader protocol

    void startRead() throws InterruptedException {
        noWritersWaiting.acquire();  // queue behind any pending writer
        readLock.acquire();
        readerCount++;
        if (readerCount == 1) {
            writeLock.acquire();     // first reader blocks writers
        }
        readLock.release();
        noWritersWaiting.release();
    }

     //Called by a reader (nurse) after finishing access to the record
    void endRead() throws InterruptedException {
        readLock.acquire();
        readerCount--;
        if (readerCount == 0) {
            writeLock.release();     // last reader unblocks writers
        }
        readLock.release();
    }

//     * Called by a writer (doctor or MLT) before modifying the record.
//     * Blocks until all active readers and other writers have finished
    void startWrite() throws InterruptedException {
        noWritersWaiting.acquire(); // signal: a writer is waiting
        writeLock.acquire();        // wait for exclusive access
        noWritersWaiting.release(); // allow next reader/writer to queue on writeLock
    }

     // Called by a writer (doctor or MLT) after completing the record update.

    void endWrite() {
        writeLock.release();
    }


//     * Returns the underlying {@link PatientRecord} for direct field access
//     * by authorised threads only (within a properly guarded write/read section)
//     * @return the patient record

    PatientRecord getRecord() { return record; }
}


// 03. Nurse (Reader)

// Represents a nurse who reads patient records to administer medications and
// monitor vitals.
// Multiple nurses may read the record simultaneously. Each nurse performs
// several read cycles with short breaks in between.

class Nurse extends Thread {

    private final int nurseId;

    private final RecordManager manager;

    private static final int READ_CYCLES = 3;

//     Constructs a Nurse thread.
//
//     @param nurseId unique nurse number (1-based)
//     @param manager the {@link RecordManager} for the patient record

    Nurse(int nurseId, RecordManager manager) {
        super("Nurse-" + nurseId);
        this.nurseId = nurseId;
        this.manager = manager;
    }


//     Repeatedly reads the patient record for {@value #READ_CYCLES} cycles.
//     Logs waiting, reading, and completion events. If interrupted at any
//      point the nurse exits cleanly.

    @Override
    public void run() {
        for (int cycle = 1; cycle <= READ_CYCLES; cycle++) {
            try {
                RecordManager.LOG.info("Nurse-" + nurseId + " is waiting to read (cycle " + cycle + ").");
                manager.startRead();

                RecordManager.LOG.info("Nurse-" + nurseId + " is READING record of "
                        + manager.getRecord().getPatientName()
                        + " | Diagnosis: " + manager.getRecord().getDiagnosis()
                        + " | Lab: "       + manager.getRecord().getLabResult());

                Thread.sleep((long) (500 + Math.random() * 1000)); // simulate reading time
                manager.endRead();

                RecordManager.LOG.info("Nurse-" + nurseId + " finished reading (cycle " + cycle + ").");
                Thread.sleep((long) (300 + Math.random() * 700));  // rest between reads

            } catch (InterruptedException e) {
                RecordManager.LOG.warning("Nurse-" + nurseId + " interrupted.");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}


// 04. Doctor (Writer)

class Doctor extends Thread {

    private final int doctorId;

    private final RecordManager manager;

    private static final int WRITE_CYCLES = 2;


//     Constructs a Doctor thread.
//
//     @param doctorId unique doctor number (1-based)
//     @param manager  the {@link RecordManager} for the patient record

    Doctor(int doctorId, RecordManager manager) {
        super("Doctor-" + doctorId);
        this.doctorId = doctorId;
        this.manager  = manager;
    }

//     Performs {@value #WRITE_CYCLES} write cycles, each time updating the
//     patient diagnosis. Logs waiting, writing, and completion events.

    @Override
    public void run() {
        for (int cycle = 1; cycle <= WRITE_CYCLES; cycle++) {
            try {
                RecordManager.LOG.info("Doctor-" + doctorId + " is waiting to write (cycle " + cycle + ").");
                manager.startWrite();

                String newDiagnosis = "Doctor-" + doctorId + " update #" + cycle
                        + ": Patient stable, prescribed medication-" + (cycle * 10);
                manager.getRecord().setDiagnosis(newDiagnosis);

                RecordManager.LOG.info("Doctor-" + doctorId + " is WRITING → " + newDiagnosis);
                Thread.sleep((long) (800 + Math.random() * 1200)); // simulate writing time

                manager.endWrite();
                RecordManager.LOG.info("Doctor-" + doctorId + " finished writing (cycle " + cycle + ").");
                Thread.sleep((long) (500 + Math.random() * 500));  // rest

            } catch (InterruptedException e) {
                RecordManager.LOG.warning("Doctor-" + doctorId + " interrupted.");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}


// 05. MLT  (Writer)


// Represents a Medical Laboratory Technician (MLT) who writes lab results into
// the patient record. Requires exclusive write access.

class MLT extends Thread {

    private final RecordManager manager;

    private static final int WRITE_CYCLES = 3;

//     * Constructs an MLT thread
//     * @param manager the {@link RecordManager} for the patient record
    MLT(RecordManager manager) {
        super("MLT-1");
        this.manager = manager;
    }


//     Performs {@value #WRITE_CYCLES} write cycles, each time updating the
//     lab result. Logs waiting, writing, and completion events.

    @Override
    public void run() {
        for (int cycle = 1; cycle <= WRITE_CYCLES; cycle++) {
            try {
                RecordManager.LOG.info("MLT is waiting to write lab results (cycle " + cycle + ").");
                manager.startWrite();

                String result = "MLT lab result #" + cycle + ": Blood glucose normal, Haemoglobin 13.5 g/dL.";
                manager.getRecord().setLabResult(result);

                RecordManager.LOG.info("MLT is WRITING → " + result);
                Thread.sleep((long) (700 + Math.random() * 1000)); // simulate writing time

                manager.endWrite();
                RecordManager.LOG.info("MLT finished writing (cycle " + cycle + ").");
                Thread.sleep((long) (400 + Math.random() * 600));  // rest

            } catch (InterruptedException e) {
                RecordManager.LOG.warning("MLT interrupted.");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}


// 06. HospitalPatientRecordSystem  (entry point)


//Entry point for the Hospital Patient Record System simulation.
//
// <p>Creates a single shared {@link PatientRecord}, wraps it in a
// {@link RecordManager}, and spawns five {@link Nurse} threads, two
// {@link Doctor} threads, and one {@link MLT} thread. All threads run
// concurrently and the main thread waits for all of them to complete before
// printing a summary.</p>

public class HospitalPatientRecordSystem {

    public static void main(String[] args) throws InterruptedException {
        // ─ Set up the shared patient record
        PatientRecord  record  = new PatientRecord("John Doe");
        RecordManager  manager = new RecordManager(record);

        // ─ Create threads
        final int NURSE_COUNT  = 5;
        final int DOCTOR_COUNT = 2;

        Thread[] nurses  = new Thread[NURSE_COUNT];
        Thread[] doctors = new Thread[DOCTOR_COUNT];
        Thread   mlt     = new MLT(manager);

        for (int i = 0; i < NURSE_COUNT;  i++) nurses[i]  = new Nurse(i + 1, manager);
        for (int i = 0; i < DOCTOR_COUNT; i++) doctors[i] = new Doctor(i + 1, manager);

        // ─ Start all threads
        for (Thread n : nurses)  n.start();
        for (Thread d : doctors) d.start();
        mlt.start();

        // ─ Wait for all threads to finish
        for (Thread n : nurses)  n.join();
        for (Thread d : doctors) d.join();
        mlt.join();

        RecordManager.LOG.info("Simulation complete. Final record for "
                + record.getPatientName()
                + " | Diagnosis: " + record.getDiagnosis()
                + " | Lab: "       + record.getLabResult());
    }
}