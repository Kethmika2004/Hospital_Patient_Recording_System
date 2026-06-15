# 🏥 Hospital Patient Record System

A Java-based concurrent programming simulation that models a **Hospital Patient Record System** using the classic **Readers-Writers Problem**.

The system demonstrates how multiple healthcare professionals can safely access a shared patient record while maintaining data consistency, fairness, and synchronization.

---

## 📌 Project Overview

This simulation represents a hospital environment where:

- **5 Nurses** act as Readers.
- **2 Doctors** act as Writers.
- **1 Medical Laboratory Technician (MLT)** acts as a Writer.
- A shared **Patient Record** stores diagnosis and laboratory results.
- Multiple nurses can read simultaneously.
- Doctors and MLT require exclusive access when updating records.

The implementation uses a **Write-Preferring Readers-Writers Solution** to ensure that writers are not starved by continuous reader activity.

---

## 🎯 Problem Statement

In a hospital information system:

- Multiple nurses frequently read patient records.
- Doctors update diagnoses.
- Laboratory technicians update test results.
- Concurrent access must not corrupt data.

The challenge is to allow:

- Concurrent reading
- Exclusive writing
- Fair scheduling
- Deadlock-free execution
- Starvation-free operation

---

# 🏗️ System Architecture

## 1️⃣ PatientRecord

Represents the shared patient record.

### Stored Information

- Patient Name
- Diagnosis
- Laboratory Results

### Responsibilities

- Store patient information
- Provide controlled access through the RecordManager

---

## 2️⃣ RecordManager

The synchronization controller of the system.

### Responsibilities

- Manage reader and writer access
- Prevent race conditions
- Implement writer-preference synchronization
- Ensure fairness using semaphores

### Semaphores Used

```java
Semaphore readLock
Semaphore writeLock
Semaphore noWritersWaiting
```

---

## 3️⃣ Nurse (Reader)

Represents healthcare staff who read patient information.

### Characteristics

- Multiple nurses can read simultaneously.
- Perform multiple read cycles.
- Never modify records.

---

## 4️⃣ Doctor (Writer)

Represents doctors updating diagnoses.

### Characteristics

- Requires exclusive access.
- Updates diagnosis information.
- Blocks readers and other writers during writing.

---

## 5️⃣ MLT (Medical Laboratory Technician)

Represents laboratory personnel.

### Characteristics

- Requires exclusive access.
- Updates laboratory results.
- Functions as a writer.

---

## 6️⃣ HospitalPatientRecordSystem

Main application entry point.

### Responsibilities

- Create shared patient record
- Create nurse, doctor, and MLT threads
- Start simulation
- Wait for all threads to complete

---

# 🔄 Readers-Writers Synchronization Strategy

## Write-Preferring Solution

This implementation prevents writer starvation using three semaphores.

### Reader Entry Protocol

```text
1. Acquire noWritersWaiting
2. Acquire readLock
3. Increment readerCount
4. First reader acquires writeLock
5. Release readLock
6. Release noWritersWaiting
```

### Reader Exit Protocol

```text
1. Acquire readLock
2. Decrement readerCount
3. Last reader releases writeLock
4. Release readLock
```

### Writer Entry Protocol

```text
1. Acquire noWritersWaiting
2. Acquire writeLock
3. Release noWritersWaiting
```

### Writer Exit Protocol

```text
1. Release writeLock
```

---

# 🔒 Concurrency Guarantees

## ✅ Mutual Exclusion

Only one writer can modify the record at a time.

No reader can access the record while a writer is active.

---

## ✅ Concurrent Reading

Multiple nurses can read the patient record simultaneously.

This improves performance and reflects real hospital workflows.

---

## ✅ Deadlock Prevention

Deadlocks are prevented because:

- Semaphores are always acquired in a consistent order.
- Circular waiting conditions are eliminated.
- Resources are released immediately after use.

---

## ✅ Starvation Prevention

The system uses:

```java
new Semaphore(1, true);
```

Fair semaphores combined with a write-preference protocol ensure:

- Writers are not blocked indefinitely.
- Readers eventually receive access.
- Every thread makes progress.

---

# 📊 Liveness Properties

| Property | Status |
|-----------|---------|
| Mutual Exclusion | ✅ Guaranteed |
| Concurrent Reading | ✅ Supported |
| Deadlock Free | ✅ Guaranteed |
| Writer Starvation Free | ✅ Guaranteed |
| Reader Starvation Free | ✅ Guaranteed |
| Fair Scheduling | ✅ Guaranteed |

---

# 🛠 Technologies Used

- Java
- Multithreading
- Semaphores
- Synchronization
- Java Logging API
- Object-Oriented Programming (OOP)

---

# 🚀 How to Run

## Clone the Repository

```bash
git clone https://github.com/yourusername/Hospital-Patient-Record-System.git
```

## Navigate to Project Folder

```bash
cd Hospital-Patient-Record-System
```

## Compile

```bash
javac HospitalPatientRecordSystem.java
```

## Run

```bash
java HospitalPatientRecordSystem
```

---

# 📷 Sample Output

```text
[INFO ] Nurse-1 is waiting to read (cycle 1).
[INFO ] Nurse-2 is waiting to read (cycle 1).

[INFO ] Nurse-1 is READING record of John Doe
         | Diagnosis: No diagnosis yet.
         | Lab: No lab results yet.

[INFO ] Doctor-1 is waiting to write (cycle 1).

[INFO ] Doctor-1 is WRITING →
         Doctor-1 update #1:
         Patient stable, prescribed medication-10

[INFO ] MLT is WRITING →
         MLT lab result #1:
         Blood glucose normal,
         Haemoglobin 13.5 g/dL.

[INFO ] Simulation complete.
```

---

# 📚 Learning Outcomes

This project demonstrates:

- Readers-Writers Problem
- Thread Synchronization
- Semaphore Usage
- Fair Scheduling
- Writer Preference Algorithms
- Concurrent Data Access Control
- Java Multithreading
- Operating System Concepts

---

# 🎓 Academic Relevance

This project is suitable for:

- Operating Systems
- Concurrent Programming
- Advanced Java Programming
- Distributed Systems
- Software Engineering
- Computer Science Coursework

---


# 📄 License

This project is released under the MIT License.

You are free to use, modify, and distribute this project for educational and research purposes.

---

# 👨‍💻 Author

**Yasandu Kethmika**

Computer Science & Engineering Undergraduate  
University of Moratuwa

GitHub: https://github.com/Kethmika2004

LinkedIn: https://www.linkedin.com/in/yasandu-kethmika-88a428337/

---

⭐ If you found this project useful, consider giving it a star on GitHub!
