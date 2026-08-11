SET NAMES utf8mb4;

INSERT INTO users (email, password_hash, full_name, phone, role, enabled, account_locked,
                   failed_login_attempts, created_at, updated_at, version)
VALUES
    ('priya.menon@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Dr. Priya Menon', '+91-9893656324', 'ADMIN', TRUE, FALSE, 0, '2025-06-20 09:00:00', '2025-06-20 09:00:00', 0),
    ('rakesh.iyer@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Prof. Rakesh Iyer', '+91-9835069727', 'ADMIN', TRUE, FALSE, 0, '2025-06-25 09:00:00', '2025-06-25 09:00:00', 0),
    ('sunita.rao@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Dr. Sunita Rao', '+91-9883579064', 'ADMIN', TRUE, FALSE, 0, '2025-06-30 09:00:00', '2025-06-30 09:00:00', 0),
    ('manoj.gupta@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Mr. Manoj Gupta', '+91-9818198896', 'ADMIN', TRUE, FALSE, 0, '2025-07-05 09:00:00', '2025-07-05 09:00:00', 0),
    ('farah.khan@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Ms. Farah Khan', '+91-9801081409', 'ADMIN', TRUE, FALSE, 0, '2025-07-10 09:00:00', '2025-07-10 09:00:00', 0),
    ('arjun.nair@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Mr. Arjun Nair', '+91-9873258621', 'ADMIN', TRUE, FALSE, 0, '2025-07-15 09:00:00', '2025-07-15 09:00:00', 0),
    ('deepa.singh@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Ms. Deepa Singh', '+91-9839691218', 'ADMIN', TRUE, FALSE, 0, '2025-07-20 09:00:00', '2025-07-20 09:00:00', 0),
    ('vikram.bose@university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Mr. Vikram Bose', '+91-9862264918', 'ADMIN', TRUE, FALSE, 0, '2025-07-25 09:00:00', '2025-07-25 09:00:00', 0)
AS new ON DUPLICATE KEY UPDATE full_name = new.full_name;

INSERT INTO admins (user_id, employee_code, department_id, designation)
SELECT u.id, x.code, d.id, x.designation FROM (
    SELECT 'priya.menon@university.edu' AS email, 'EMP-1001' AS code, 'CSE' AS dept, 'Head of Department' AS designation
    UNION ALL
    SELECT 'rakesh.iyer@university.edu' AS email, 'EMP-1002' AS code, 'ECE' AS dept, 'Associate Professor' AS designation
    UNION ALL
    SELECT 'sunita.rao@university.edu' AS email, 'EMP-1003' AS code, 'EXAM' AS dept, 'Controller of Examinations' AS designation
    UNION ALL
    SELECT 'manoj.gupta@university.edu' AS email, 'EMP-1004' AS code, 'HOSTEL' AS dept, 'Chief Warden' AS designation
    UNION ALL
    SELECT 'farah.khan@university.edu' AS email, 'EMP-1005' AS code, 'LIBRARY' AS dept, 'Chief Librarian' AS designation
    UNION ALL
    SELECT 'arjun.nair@university.edu' AS email, 'EMP-1006' AS code, 'IT' AS dept, 'Systems Administrator' AS designation
    UNION ALL
    SELECT 'deepa.singh@university.edu' AS email, 'EMP-1007' AS code, 'ACCOUNTS' AS dept, 'Accounts Officer' AS designation
    UNION ALL
    SELECT 'vikram.bose@university.edu' AS email, 'EMP-1008' AS code, 'TRANSPORT' AS dept, 'Transport Manager' AS designation
) AS x
JOIN users u ON u.email = x.email
JOIN departments d ON d.code = x.dept
ON DUPLICATE KEY UPDATE designation = VALUES(designation);

INSERT INTO users (email, password_hash, full_name, phone, role, enabled, account_locked,
                   failed_login_attempts, created_at, updated_at, version)
VALUES
    ('aarav.sharma1@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Aarav Sharma', '+91-9733749348', 'STUDENT', TRUE, FALSE, 0, '2025-09-28 09:00:00', '2025-09-28 09:00:00', 0),
    ('ananya.joshi2@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Ananya Joshi', '+91-9722440607', 'STUDENT', TRUE, FALSE, 0, '2025-10-01 09:00:00', '2025-10-01 09:00:00', 0),
    ('rohit.agarwal3@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Rohit Agarwal', '+91-9716892356', 'STUDENT', TRUE, FALSE, 0, '2025-10-04 09:00:00', '2025-10-04 09:00:00', 0),
    ('sneha.das4@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Sneha Das', '+91-9707985922', 'STUDENT', TRUE, FALSE, 0, '2025-10-07 09:00:00', '2025-10-07 09:00:00', 0),
    ('kabir.sinha5@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Kabir Sinha', '+91-9741723143', 'STUDENT', TRUE, FALSE, 0, '2025-10-10 09:00:00', '2025-10-10 09:00:00', 0),
    ('ishita.reddy6@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Ishita Reddy', '+91-9708251793', 'STUDENT', TRUE, FALSE, 0, '2025-10-13 09:00:00', '2025-10-13 09:00:00', 0),
    ('vivaan.pillai7@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Vivaan Pillai', '+91-9791481564', 'STUDENT', TRUE, FALSE, 0, '2025-10-16 09:00:00', '2025-10-16 09:00:00', 0),
    ('meera.verma8@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Meera Verma', '+91-9774659838', 'STUDENT', TRUE, FALSE, 0, '2025-10-19 09:00:00', '2025-10-19 09:00:00', 0),
    ('aditya.mehta9@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Aditya Mehta', '+91-9725723674', 'STUDENT', TRUE, FALSE, 0, '2025-10-22 09:00:00', '2025-10-22 09:00:00', 0),
    ('nisha.fernandes10@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Nisha Fernandes', '+91-9775638100', 'STUDENT', TRUE, FALSE, 0, '2025-10-25 09:00:00', '2025-10-25 09:00:00', 0),
    ('karan.kulkarni11@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Karan Kulkarni', '+91-9763026959', 'STUDENT', TRUE, FALSE, 0, '2025-10-28 09:00:00', '2025-10-28 09:00:00', 0),
    ('riya.kaur12@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Riya Kaur', '+91-9729628783', 'STUDENT', TRUE, FALSE, 0, '2025-10-31 09:00:00', '2025-10-31 09:00:00', 0),
    ('siddharth.nair13@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Siddharth Nair', '+91-9751560019', 'STUDENT', TRUE, FALSE, 0, '2025-11-03 09:00:00', '2025-11-03 09:00:00', 0),
    ('tanvi.bhatt14@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Tanvi Bhatt', '+91-9709342633', 'STUDENT', TRUE, FALSE, 0, '2025-11-06 09:00:00', '2025-11-06 09:00:00', 0),
    ('harsh.patel15@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Harsh Patel', '+91-9799427668', 'STUDENT', TRUE, FALSE, 0, '2025-11-09 09:00:00', '2025-11-09 09:00:00', 0),
    ('pooja.chatterjee16@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Pooja Chatterjee', '+91-9735744042', 'STUDENT', TRUE, FALSE, 0, '2025-11-12 09:00:00', '2025-11-12 09:00:00', 0),
    ('nikhil.sharma17@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Nikhil Sharma', '+91-9779180489', 'STUDENT', TRUE, FALSE, 0, '2025-11-15 09:00:00', '2025-11-15 09:00:00', 0),
    ('divya.joshi18@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Divya Joshi', '+91-9788584007', 'STUDENT', TRUE, FALSE, 0, '2025-11-18 09:00:00', '2025-11-18 09:00:00', 0),
    ('rahul.agarwal19@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Rahul Agarwal', '+91-9784536868', 'STUDENT', TRUE, FALSE, 0, '2025-11-21 09:00:00', '2025-11-21 09:00:00', 0),
    ('shreya.das20@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Shreya Das', '+91-9785048948', 'STUDENT', TRUE, FALSE, 0, '2025-11-24 09:00:00', '2025-11-24 09:00:00', 0),
    ('aryan.sinha21@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Aryan Sinha', '+91-9734515977', 'STUDENT', TRUE, FALSE, 0, '2025-11-27 09:00:00', '2025-11-27 09:00:00', 0),
    ('kavya.reddy22@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Kavya Reddy', '+91-9757927740', 'STUDENT', TRUE, FALSE, 0, '2025-11-30 09:00:00', '2025-11-30 09:00:00', 0),
    ('manav.pillai23@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Manav Pillai', '+91-9743148976', 'STUDENT', TRUE, FALSE, 0, '2025-12-03 09:00:00', '2025-12-03 09:00:00', 0),
    ('neha.verma24@student.university.edu', '$2a$10$p4KiST1Z2uCvcBAvQXaedepGRB/4dR4UDPgzo/CFw71C2bIyKIDMq', 'Neha Verma', '+91-9700324332', 'STUDENT', TRUE, FALSE, 0, '2025-12-06 09:00:00', '2025-12-06 09:00:00', 0)
AS new ON DUPLICATE KEY UPDATE full_name = new.full_name;

INSERT INTO students (user_id, roll_number, department_id, program, batch_year, semester)
SELECT u.id, x.roll, d.id, x.program, x.batch_year, x.semester FROM (
    SELECT 'aarav.sharma1@student.university.edu' AS email, '2022CS1000' AS roll, 'CSE' AS dept, 'B.Tech' AS program, 2022 AS batch_year, 1 AS semester
    UNION ALL
    SELECT 'ananya.joshi2@student.university.edu' AS email, '2023CS1001' AS roll, 'CSE' AS dept, 'B.Tech' AS program, 2023 AS batch_year, 2 AS semester
    UNION ALL
    SELECT 'rohit.agarwal3@student.university.edu' AS email, '2024CS1002' AS roll, 'CSE' AS dept, 'B.Tech' AS program, 2024 AS batch_year, 3 AS semester
    UNION ALL
    SELECT 'sneha.das4@student.university.edu' AS email, '2022EC1003' AS roll, 'ECE' AS dept, 'M.Tech' AS program, 2022 AS batch_year, 4 AS semester
    UNION ALL
    SELECT 'kabir.sinha5@student.university.edu' AS email, '2023EC1004' AS roll, 'ECE' AS dept, 'BCA' AS program, 2023 AS batch_year, 5 AS semester
    UNION ALL
    SELECT 'ishita.reddy6@student.university.edu' AS email, '2024ME1005' AS roll, 'MECH' AS dept, 'MCA' AS program, 2024 AS batch_year, 6 AS semester
    UNION ALL
    SELECT 'vivaan.pillai7@student.university.edu' AS email, '2022CI1006' AS roll, 'CIVIL' AS dept, 'B.Tech' AS program, 2022 AS batch_year, 7 AS semester
    UNION ALL
    SELECT 'meera.verma8@student.university.edu' AS email, '2023CS1007' AS roll, 'CSE' AS dept, 'B.Tech' AS program, 2023 AS batch_year, 8 AS semester
    UNION ALL
    SELECT 'aditya.mehta9@student.university.edu' AS email, '2024CS1008' AS roll, 'CSE' AS dept, 'B.Tech' AS program, 2024 AS batch_year, 1 AS semester
    UNION ALL
    SELECT 'nisha.fernandes10@student.university.edu' AS email, '2022CS1009' AS roll, 'CSE' AS dept, 'M.Tech' AS program, 2022 AS batch_year, 2 AS semester
    UNION ALL
    SELECT 'karan.kulkarni11@student.university.edu' AS email, '2023EC1010' AS roll, 'ECE' AS dept, 'BCA' AS program, 2023 AS batch_year, 3 AS semester
    UNION ALL
    SELECT 'riya.kaur12@student.university.edu' AS email, '2024EC1011' AS roll, 'ECE' AS dept, 'MCA' AS program, 2024 AS batch_year, 4 AS semester
    UNION ALL
    SELECT 'siddharth.nair13@student.university.edu' AS email, '2022ME1012' AS roll, 'MECH' AS dept, 'B.Tech' AS program, 2022 AS batch_year, 5 AS semester
    UNION ALL
    SELECT 'tanvi.bhatt14@student.university.edu' AS email, '2023CI1013' AS roll, 'CIVIL' AS dept, 'B.Tech' AS program, 2023 AS batch_year, 6 AS semester
    UNION ALL
    SELECT 'harsh.patel15@student.university.edu' AS email, '2024CS1014' AS roll, 'CSE' AS dept, 'B.Tech' AS program, 2024 AS batch_year, 7 AS semester
    UNION ALL
    SELECT 'pooja.chatterjee16@student.university.edu' AS email, '2022CS1015' AS roll, 'CSE' AS dept, 'M.Tech' AS program, 2022 AS batch_year, 8 AS semester
    UNION ALL
    SELECT 'nikhil.sharma17@student.university.edu' AS email, '2023CS1016' AS roll, 'CSE' AS dept, 'BCA' AS program, 2023 AS batch_year, 1 AS semester
    UNION ALL
    SELECT 'divya.joshi18@student.university.edu' AS email, '2024EC1017' AS roll, 'ECE' AS dept, 'MCA' AS program, 2024 AS batch_year, 2 AS semester
    UNION ALL
    SELECT 'rahul.agarwal19@student.university.edu' AS email, '2022EC1018' AS roll, 'ECE' AS dept, 'B.Tech' AS program, 2022 AS batch_year, 3 AS semester
    UNION ALL
    SELECT 'shreya.das20@student.university.edu' AS email, '2023ME1019' AS roll, 'MECH' AS dept, 'B.Tech' AS program, 2023 AS batch_year, 4 AS semester
    UNION ALL
    SELECT 'aryan.sinha21@student.university.edu' AS email, '2024CI1020' AS roll, 'CIVIL' AS dept, 'B.Tech' AS program, 2024 AS batch_year, 5 AS semester
    UNION ALL
    SELECT 'kavya.reddy22@student.university.edu' AS email, '2022CS1021' AS roll, 'CSE' AS dept, 'M.Tech' AS program, 2022 AS batch_year, 6 AS semester
    UNION ALL
    SELECT 'manav.pillai23@student.university.edu' AS email, '2023CS1022' AS roll, 'CSE' AS dept, 'BCA' AS program, 2023 AS batch_year, 7 AS semester
    UNION ALL
    SELECT 'neha.verma24@student.university.edu' AS email, '2024CS1023' AS roll, 'CSE' AS dept, 'MCA' AS program, 2024 AS batch_year, 8 AS semester
) AS x
JOIN users u ON u.email = x.email
JOIN departments d ON d.code = x.dept
ON DUPLICATE KEY UPDATE program = VALUES(program);

INSERT INTO feedback (ticket_number, title, description, category, priority, status,
                      submitted_by_id, assigned_to_id, department_id, anonymous,
                      sentiment_label, sentiment_score, suggested_category, suggested_priority,
                      analysis_confidence, analysed_at, due_at, resolved_at, closed_at,
                      satisfaction_rating, created_at, updated_at, version)
SELECT x.ticket, x.title, x.descr, x.category, x.priority, x.status,
       s.user_id, a.user_id, d.id, x.anonymous,
       x.sentiment_label, x.sentiment_score, x.category, x.priority,
       x.confidence, x.created_at, x.due_at, x.resolved_at, x.closed_at,
       x.rating, x.created_at, COALESCE(x.closed_at, x.resolved_at, x.created_at), 0
FROM (
    SELECT 'FB-2026-000001' AS ticket, 'Lab computers extremely slow in Block C' AS title, 'The machines in the Block C programming lab take almost ten minutes to boot and the IDE freezes constantly. Half the two-hour slot is gone before anyone can start the actual exercise. This has been happening for three weeks now.' AS descr, 'IT_SUPPORT' AS category, 'HIGH' AS priority, 'CLOSED' AS status, 'aarav.sharma1@student.university.edu' AS student_email, 'arjun.nair@university.edu' AS admin_email, 'IT' AS dept, 1 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.72 AS sentiment_score, 0.81 AS confidence, '2026-07-04 20:00:00' AS created_at, '2026-07-07 20:00:00' AS due_at, '2026-07-06 12:00:00' AS resolved_at, '2026-07-09 12:00:00' AS closed_at, 4 AS rating
    UNION ALL
    SELECT 'FB-2026-000002' AS ticket, 'Mess food quality has dropped sharply this month' AS title, 'The dinner served in the boys hostel mess has been repetitive and often undercooked since the vendor changed. Several students have reported stomach problems. Requesting an inspection of the kitchen.' AS descr, 'HOSTEL' AS category, 'URGENT' AS priority, 'RESOLVED' AS status, 'ananya.joshi2@student.university.edu' AS student_email, 'manoj.gupta@university.edu' AS admin_email, 'HOSTEL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.85 AS sentiment_score, 0.85 AS confidence, '2026-04-28 07:00:00' AS created_at, '2026-04-29 07:00:00' AS due_at, '2026-05-01 17:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000003' AS ticket, 'Revaluation results for Semester 5 still not published' AS title, 'It has been six weeks since revaluation applications closed for the Semester 5 Data Structures paper. The portal still shows the original marks and there is no announcement about when the revised results will appear.' AS descr, 'EXAMINATION' AS category, 'HIGH' AS priority, 'OPEN' AS status, 'rohit.agarwal3@student.university.edu' AS student_email, NULL AS admin_email, 'EXAM' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.58 AS sentiment_score, 0.91 AS confidence, '2026-02-25 15:00:00' AS created_at, '2026-02-28 15:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'Library reading room closes too early during exams' AS title, 'During the end-semester examination period the central reading room shuts at 8 PM. Many of us commute and cannot study at home. Extending the hours to 11 PM during exam weeks would help a lot.' AS descr, 'LIBRARY' AS category, 'MEDIUM' AS priority, 'CLOSED' AS status, 'sneha.das4@student.university.edu' AS student_email, 'farah.khan@university.edu' AS admin_email, 'LIBRARY' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, -0.15 AS sentiment_score, 0.83 AS confidence, '2026-04-16 17:00:00' AS created_at, '2026-04-23 17:00:00' AS due_at, '2026-04-28 02:00:00' AS resolved_at, '2026-05-02 02:00:00' AS closed_at, 5 AS rating
    UNION ALL
    SELECT 'FB-2026-000005' AS ticket, 'Excellent handling of the network outage last week' AS title, 'I want to appreciate the IT team for restoring campus Wi-Fi within a few hours during the fibre cut last Tuesday. Updates were posted regularly and the temporary hotspot in the library was very helpful.' AS descr, 'IT_SUPPORT' AS category, 'LOW' AS priority, 'IN_PROGRESS' AS status, 'kabir.sinha5@student.university.edu' AS student_email, 'arjun.nair@university.edu' AS admin_email, 'IT' AS dept, 0 AS anonymous, 'POSITIVE' AS sentiment_label, 0.88 AS sentiment_score, 0.96 AS confidence, '2026-07-08 19:00:00' AS created_at, '2026-07-22 19:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000006' AS ticket, 'Route 7 college bus consistently arrives 25 minutes late' AS title, 'The 7:40 AM bus on Route 7 from Kondapur has been arriving between 8:05 and 8:15 every day this month. Students on this route are missing the first period regularly.' AS descr, 'TRANSPORT' AS category, 'HIGH' AS priority, 'OPEN' AS status, 'ishita.reddy6@student.university.edu' AS student_email, NULL AS admin_email, 'TRANSPORT' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.66 AS sentiment_score, 0.92 AS confidence, '2026-06-29 19:00:00' AS created_at, '2026-07-02 19:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000007' AS ticket, 'Scholarship reimbursement pending since March' AS title, 'My merit scholarship reimbursement for the current academic year was approved in March but has not been credited. The accounts counter keeps asking me to come back next week.' AS descr, 'ADMINISTRATION' AS category, 'HIGH' AS priority, 'OPEN' AS status, 'vivaan.pillai7@student.university.edu' AS student_email, NULL AS admin_email, 'ACCOUNTS' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.61 AS sentiment_score, 0.82 AS confidence, '2026-05-02 20:00:00' AS created_at, '2026-05-05 20:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'Request for additional Python tutorial sessions' AS title, 'Many students in our batch are struggling with the pace of the Python module. Could the department arrange two extra tutorial hours per week before the internal assessment?' AS descr, 'ACADEMIC' AS category, 'MEDIUM' AS priority, 'CLOSED' AS status, 'meera.verma8@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CSE' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, -0.08 AS sentiment_score, 0.71 AS confidence, '2026-04-04 19:00:00' AS created_at, '2026-04-11 19:00:00' AS due_at, '2026-04-14 11:00:00' AS resolved_at, '2026-04-18 11:00:00' AS closed_at, 2 AS rating
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'Broken chairs and non-functional fans in Room 204' AS title, 'Room 204 in the main academic block has six broken chairs and three of the five ceiling fans do not work. In this weather the room becomes unusable by midday.' AS descr, 'INFRASTRUCTURE' AS category, 'MEDIUM' AS priority, 'CLOSED' AS status, 'aditya.mehta9@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CIVIL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.54 AS sentiment_score, 0.95 AS confidence, '2026-07-09 06:00:00' AS created_at, '2026-07-16 06:00:00' AS due_at, '2026-07-18 10:00:00' AS resolved_at, '2026-07-19 10:00:00' AS closed_at, 4 AS rating
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'Digital library subscription to IEEE not accessible off campus' AS title, 'The IEEE Xplore subscription only works from campus IP addresses. Final-year students working on projects from home cannot download papers. A VPN or remote proxy would solve this.' AS descr, 'LIBRARY' AS category, 'MEDIUM' AS priority, 'RESOLVED' AS status, 'nisha.fernandes10@student.university.edu' AS student_email, 'farah.khan@university.edu' AS admin_email, 'LIBRARY' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, -0.22 AS sentiment_score, 0.9 AS confidence, '2026-03-08 13:00:00' AS created_at, '2026-03-15 13:00:00' AS due_at, '2026-03-19 17:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000011' AS ticket, 'Faculty feedback: outstanding Signals and Systems teaching' AS title, 'Prof. Iyer''s Signals and Systems lectures have been exceptional this semester. The worked examples and the extra doubt-clearing sessions on Saturdays have made a genuinely difficult subject approachable.' AS descr, 'FACULTY' AS category, 'LOW' AS priority, 'RESOLVED' AS status, 'karan.kulkarni11@student.university.edu' AS student_email, 'rakesh.iyer@university.edu' AS admin_email, 'ECE' AS dept, 0 AS anonymous, 'POSITIVE' AS sentiment_label, 0.92 AS sentiment_score, 0.85 AS confidence, '2026-04-02 04:00:00' AS created_at, '2026-04-16 04:00:00' AS due_at, '2026-04-09 21:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000012' AS ticket, 'Hostel water supply cut off every afternoon' AS title, 'Water supply in Hostel Block B stops between 1 PM and 5 PM almost daily. Students with afternoon labs cannot wash up before evening classes.' AS descr, 'HOSTEL' AS category, 'HIGH' AS priority, 'IN_PROGRESS' AS status, 'riya.kaur12@student.university.edu' AS student_email, 'manoj.gupta@university.edu' AS admin_email, 'HOSTEL' AS dept, 1 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.7 AS sentiment_score, 0.82 AS confidence, '2026-04-01 14:00:00' AS created_at, '2026-04-04 14:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000013' AS ticket, 'Internal marks not displayed on the portal' AS title, 'Internal assessment marks for three subjects are missing from the student portal even though the assessments were completed a month ago.' AS descr, 'EXAMINATION' AS category, 'MEDIUM' AS priority, 'OPEN' AS status, 'siddharth.nair13@student.university.edu' AS student_email, NULL AS admin_email, 'EXAM' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.41 AS sentiment_score, 0.74 AS confidence, '2026-03-23 00:00:00' AS created_at, '2026-03-30 00:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'Workshop machinery lacks safety guards' AS title, 'Two of the lathe machines in the mechanical workshop are missing their chip guards. This is a real injury risk during the practical sessions.' AS descr, 'INFRASTRUCTURE' AS category, 'URGENT' AS priority, 'CLOSED' AS status, 'tanvi.bhatt14@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'MECH' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.79 AS sentiment_score, 0.91 AS confidence, '2026-07-19 07:00:00' AS created_at, '2026-07-20 07:00:00' AS due_at, '2026-07-21 16:00:00' AS resolved_at, '2026-07-25 16:00:00' AS closed_at, 4 AS rating
    UNION ALL
    SELECT 'FB-2026-000015' AS ticket, 'Suggestion: publish the academic calendar earlier' AS title, 'The academic calendar for the next semester is usually released only a week before classes begin. Publishing it a month in advance would help students plan internships and travel.' AS descr, 'ADMINISTRATION' AS category, 'LOW' AS priority, 'OPEN' AS status, 'harsh.patel15@student.university.edu' AS student_email, NULL AS admin_email, 'EXAM' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, 0.05 AS sentiment_score, 0.72 AS confidence, '2026-03-21 16:00:00' AS created_at, '2026-04-04 16:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000016' AS ticket, 'Wi-Fi dead zones in the new academic block' AS title, 'Floors 3 and 4 of the new academic block have no usable Wi-Fi signal. Online quizzes conducted in those classrooms keep disconnecting.' AS descr, 'IT_SUPPORT' AS category, 'MEDIUM' AS priority, 'IN_PROGRESS' AS status, 'pooja.chatterjee16@student.university.edu' AS student_email, 'arjun.nair@university.edu' AS admin_email, 'IT' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.49 AS sentiment_score, 0.74 AS confidence, '2026-05-05 10:00:00' AS created_at, '2026-05-12 10:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'Placement preparation sessions clash with lab hours' AS title, 'The aptitude training sessions organised by the placement cell are scheduled at the same time as the Semester 6 database lab. We are being forced to choose between the two.' AS descr, 'ACADEMIC' AS category, 'MEDIUM' AS priority, 'CLOSED' AS status, 'nikhil.sharma17@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CSE' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.37 AS sentiment_score, 0.82 AS confidence, '2026-05-20 01:00:00' AS created_at, '2026-05-27 01:00:00' AS due_at, '2026-05-23 05:00:00' AS resolved_at, '2026-05-27 05:00:00' AS closed_at, 5 AS rating
    UNION ALL
    SELECT 'FB-2026-000018' AS ticket, 'Thanks for the new library book acquisitions' AS title, 'The recently added algorithms and machine learning titles are exactly what final-year students needed. Please continue expanding this section.' AS descr, 'LIBRARY' AS category, 'LOW' AS priority, 'AWAITING_STUDENT' AS status, 'divya.joshi18@student.university.edu' AS student_email, 'farah.khan@university.edu' AS admin_email, 'LIBRARY' AS dept, 0 AS anonymous, 'POSITIVE' AS sentiment_label, 0.81 AS sentiment_score, 0.81 AS confidence, '2026-03-19 09:00:00' AS created_at, '2026-04-02 09:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000019' AS ticket, 'Fee receipt shows incorrect amount' AS title, 'My hostel fee receipt for this semester shows an amount 8,000 higher than the notified fee structure. I have attached the notification and the receipt.' AS descr, 'ADMINISTRATION' AS category, 'HIGH' AS priority, 'RESOLVED' AS status, 'rahul.agarwal19@student.university.edu' AS student_email, 'deepa.singh@university.edu' AS admin_email, 'ACCOUNTS' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.55 AS sentiment_score, 0.77 AS confidence, '2026-06-29 23:00:00' AS created_at, '2026-07-02 23:00:00' AS due_at, '2026-07-04 16:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000020' AS ticket, 'Projector in Seminar Hall 2 not working' AS title, 'The projector in Seminar Hall 2 has not worked for the last four sessions. Guest lectures are being conducted without slides.' AS descr, 'INFRASTRUCTURE' AS category, 'MEDIUM' AS priority, 'AWAITING_STUDENT' AS status, 'shreya.das20@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CIVIL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.44 AS sentiment_score, 0.89 AS confidence, '2026-05-16 02:00:00' AS created_at, '2026-05-23 02:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'Lab computers extremely slow in Block C (reported again)' AS title, 'The machines in the Block C programming lab take almost ten minutes to boot and the IDE freezes constantly. Half the two-hour slot is gone before anyone can start the actual exercise. This has been happening for three weeks now.' AS descr, 'IT_SUPPORT' AS category, 'HIGH' AS priority, 'CLOSED' AS status, 'aryan.sinha21@student.university.edu' AS student_email, 'arjun.nair@university.edu' AS admin_email, 'IT' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.72 AS sentiment_score, 0.96 AS confidence, '2026-02-27 16:00:00' AS created_at, '2026-03-02 16:00:00' AS due_at, '2026-03-04 21:00:00' AS resolved_at, '2026-03-10 21:00:00' AS closed_at, 2 AS rating
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'Mess food quality has dropped sharply this month (reported again)' AS title, 'The dinner served in the boys hostel mess has been repetitive and often undercooked since the vendor changed. Several students have reported stomach problems. Requesting an inspection of the kitchen.' AS descr, 'HOSTEL' AS category, 'URGENT' AS priority, 'CLOSED' AS status, 'kavya.reddy22@student.university.edu' AS student_email, 'manoj.gupta@university.edu' AS admin_email, 'HOSTEL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.85 AS sentiment_score, 0.81 AS confidence, '2026-04-30 03:00:00' AS created_at, '2026-05-01 03:00:00' AS due_at, '2026-05-05 13:00:00' AS resolved_at, '2026-05-10 13:00:00' AS closed_at, 5 AS rating
    UNION ALL
    SELECT 'FB-2026-000023' AS ticket, 'Revaluation results for Semester 5 still not published (reported again)' AS title, 'It has been six weeks since revaluation applications closed for the Semester 5 Data Structures paper. The portal still shows the original marks and there is no announcement about when the revised results will appear.' AS descr, 'EXAMINATION' AS category, 'HIGH' AS priority, 'IN_PROGRESS' AS status, 'manav.pillai23@student.university.edu' AS student_email, 'sunita.rao@university.edu' AS admin_email, 'EXAM' AS dept, 1 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.58 AS sentiment_score, 0.79 AS confidence, '2026-05-29 11:00:00' AS created_at, '2026-06-01 11:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000024' AS ticket, 'Library reading room closes too early during exams (reported again)' AS title, 'During the end-semester examination period the central reading room shuts at 8 PM. Many of us commute and cannot study at home. Extending the hours to 11 PM during exam weeks would help a lot.' AS descr, 'LIBRARY' AS category, 'MEDIUM' AS priority, 'RESOLVED' AS status, 'neha.verma24@student.university.edu' AS student_email, 'farah.khan@university.edu' AS admin_email, 'LIBRARY' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, -0.15 AS sentiment_score, 0.71 AS confidence, '2026-07-12 15:00:00' AS created_at, '2026-07-19 15:00:00' AS due_at, '2026-07-22 19:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'Excellent handling of the network outage last week (reported again)' AS title, 'I want to appreciate the IT team for restoring campus Wi-Fi within a few hours during the fibre cut last Tuesday. Updates were posted regularly and the temporary hotspot in the library was very helpful.' AS descr, 'IT_SUPPORT' AS category, 'LOW' AS priority, 'RESOLVED' AS status, 'aarav.sharma1@student.university.edu' AS student_email, 'arjun.nair@university.edu' AS admin_email, 'IT' AS dept, 0 AS anonymous, 'POSITIVE' AS sentiment_label, 0.88 AS sentiment_score, 0.81 AS confidence, '2026-03-11 12:00:00' AS created_at, '2026-03-25 12:00:00' AS due_at, '2026-03-21 17:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000026' AS ticket, 'Route 7 college bus consistently arrives 25 minutes late (reported again)' AS title, 'The 7:40 AM bus on Route 7 from Kondapur has been arriving between 8:05 and 8:15 every day this month. Students on this route are missing the first period regularly.' AS descr, 'TRANSPORT' AS category, 'HIGH' AS priority, 'OPEN' AS status, 'ananya.joshi2@student.university.edu' AS student_email, NULL AS admin_email, 'TRANSPORT' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.66 AS sentiment_score, 0.93 AS confidence, '2026-06-25 20:00:00' AS created_at, '2026-06-28 20:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000027' AS ticket, 'Scholarship reimbursement pending since March (reported again)' AS title, 'My merit scholarship reimbursement for the current academic year was approved in March but has not been credited. The accounts counter keeps asking me to come back next week.' AS descr, 'ADMINISTRATION' AS category, 'HIGH' AS priority, 'OPEN' AS status, 'rohit.agarwal3@student.university.edu' AS student_email, NULL AS admin_email, 'ACCOUNTS' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.61 AS sentiment_score, 0.97 AS confidence, '2026-05-15 18:00:00' AS created_at, '2026-05-18 18:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000028' AS ticket, 'Request for additional Python tutorial sessions (reported again)' AS title, 'Many students in our batch are struggling with the pace of the Python module. Could the department arrange two extra tutorial hours per week before the internal assessment?' AS descr, 'ACADEMIC' AS category, 'MEDIUM' AS priority, 'IN_PROGRESS' AS status, 'sneha.das4@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CSE' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, -0.08 AS sentiment_score, 0.87 AS confidence, '2026-04-26 17:00:00' AS created_at, '2026-05-03 17:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000029' AS ticket, 'Broken chairs and non-functional fans in Room 204 (reported again)' AS title, 'Room 204 in the main academic block has six broken chairs and three of the five ceiling fans do not work. In this weather the room becomes unusable by midday.' AS descr, 'INFRASTRUCTURE' AS category, 'MEDIUM' AS priority, 'IN_PROGRESS' AS status, 'kabir.sinha5@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CIVIL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.54 AS sentiment_score, 0.89 AS confidence, '2026-07-11 01:00:00' AS created_at, '2026-07-18 01:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'Digital library subscription to IEEE not accessible off campus (reported again)' AS title, 'The IEEE Xplore subscription only works from campus IP addresses. Final-year students working on projects from home cannot download papers. A VPN or remote proxy would solve this.' AS descr, 'LIBRARY' AS category, 'MEDIUM' AS priority, 'CLOSED' AS status, 'ishita.reddy6@student.university.edu' AS student_email, 'farah.khan@university.edu' AS admin_email, 'LIBRARY' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, -0.22 AS sentiment_score, 0.74 AS confidence, '2026-07-18 07:00:00' AS created_at, '2026-07-25 07:00:00' AS due_at, '2026-07-27 19:00:00' AS resolved_at, '2026-08-02 19:00:00' AS closed_at, 4 AS rating
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'Faculty feedback: outstanding Signals and Systems teaching (reported again)' AS title, 'Prof. Iyer''s Signals and Systems lectures have been exceptional this semester. The worked examples and the extra doubt-clearing sessions on Saturdays have made a genuinely difficult subject approachable.' AS descr, 'FACULTY' AS category, 'LOW' AS priority, 'CLOSED' AS status, 'vivaan.pillai7@student.university.edu' AS student_email, 'rakesh.iyer@university.edu' AS admin_email, 'ECE' AS dept, 0 AS anonymous, 'POSITIVE' AS sentiment_label, 0.92 AS sentiment_score, 0.86 AS confidence, '2026-06-22 08:00:00' AS created_at, '2026-07-06 08:00:00' AS due_at, '2026-07-06 14:00:00' AS resolved_at, '2026-07-07 14:00:00' AS closed_at, 2 AS rating
    UNION ALL
    SELECT 'FB-2026-000032' AS ticket, 'Hostel water supply cut off every afternoon (reported again)' AS title, 'Water supply in Hostel Block B stops between 1 PM and 5 PM almost daily. Students with afternoon labs cannot wash up before evening classes.' AS descr, 'HOSTEL' AS category, 'HIGH' AS priority, 'AWAITING_STUDENT' AS status, 'meera.verma8@student.university.edu' AS student_email, 'manoj.gupta@university.edu' AS admin_email, 'HOSTEL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.7 AS sentiment_score, 0.85 AS confidence, '2026-05-23 02:00:00' AS created_at, '2026-05-26 02:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000033' AS ticket, 'Internal marks not displayed on the portal (reported again)' AS title, 'Internal assessment marks for three subjects are missing from the student portal even though the assessments were completed a month ago.' AS descr, 'EXAMINATION' AS category, 'MEDIUM' AS priority, 'IN_PROGRESS' AS status, 'aditya.mehta9@student.university.edu' AS student_email, 'sunita.rao@university.edu' AS admin_email, 'EXAM' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.41 AS sentiment_score, 0.92 AS confidence, '2026-04-03 10:00:00' AS created_at, '2026-04-10 10:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'Workshop machinery lacks safety guards (reported again)' AS title, 'Two of the lathe machines in the mechanical workshop are missing their chip guards. This is a real injury risk during the practical sessions.' AS descr, 'INFRASTRUCTURE' AS category, 'URGENT' AS priority, 'CLOSED' AS status, 'nisha.fernandes10@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'MECH' AS dept, 1 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.79 AS sentiment_score, 0.79 AS confidence, '2026-03-04 13:00:00' AS created_at, '2026-03-05 13:00:00' AS due_at, '2026-03-06 04:00:00' AS resolved_at, '2026-03-11 04:00:00' AS closed_at, 4 AS rating
    UNION ALL
    SELECT 'FB-2026-000035' AS ticket, 'Suggestion: publish the academic calendar earlier (reported again)' AS title, 'The academic calendar for the next semester is usually released only a week before classes begin. Publishing it a month in advance would help students plan internships and travel.' AS descr, 'ADMINISTRATION' AS category, 'LOW' AS priority, 'OPEN' AS status, 'karan.kulkarni11@student.university.edu' AS student_email, NULL AS admin_email, 'EXAM' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, 0.05 AS sentiment_score, 0.86 AS confidence, '2026-06-01 01:00:00' AS created_at, '2026-06-15 01:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'Wi-Fi dead zones in the new academic block (reported again)' AS title, 'Floors 3 and 4 of the new academic block have no usable Wi-Fi signal. Online quizzes conducted in those classrooms keep disconnecting.' AS descr, 'IT_SUPPORT' AS category, 'MEDIUM' AS priority, 'CLOSED' AS status, 'riya.kaur12@student.university.edu' AS student_email, 'arjun.nair@university.edu' AS admin_email, 'IT' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.49 AS sentiment_score, 0.8 AS confidence, '2026-06-11 05:00:00' AS created_at, '2026-06-18 05:00:00' AS due_at, '2026-06-15 18:00:00' AS resolved_at, '2026-06-19 18:00:00' AS closed_at, 2 AS rating
    UNION ALL
    SELECT 'FB-2026-000037' AS ticket, 'Placement preparation sessions clash with lab hours (reported again)' AS title, 'The aptitude training sessions organised by the placement cell are scheduled at the same time as the Semester 6 database lab. We are being forced to choose between the two.' AS descr, 'ACADEMIC' AS category, 'MEDIUM' AS priority, 'REJECTED' AS status, 'siddharth.nair13@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CSE' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.37 AS sentiment_score, 0.91 AS confidence, '2026-04-04 16:00:00' AS created_at, '2026-04-11 16:00:00' AS due_at, NULL AS resolved_at, '2026-04-09 16:00:00' AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'Thanks for the new library book acquisitions (reported again)' AS title, 'The recently added algorithms and machine learning titles are exactly what final-year students needed. Please continue expanding this section.' AS descr, 'LIBRARY' AS category, 'LOW' AS priority, 'RESOLVED' AS status, 'tanvi.bhatt14@student.university.edu' AS student_email, 'farah.khan@university.edu' AS admin_email, 'LIBRARY' AS dept, 0 AS anonymous, 'POSITIVE' AS sentiment_label, 0.81 AS sentiment_score, 0.89 AS confidence, '2026-04-03 14:00:00' AS created_at, '2026-04-17 14:00:00' AS due_at, '2026-04-10 00:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000039' AS ticket, 'Fee receipt shows incorrect amount (reported again)' AS title, 'My hostel fee receipt for this semester shows an amount 8,000 higher than the notified fee structure. I have attached the notification and the receipt.' AS descr, 'ADMINISTRATION' AS category, 'HIGH' AS priority, 'OPEN' AS status, 'harsh.patel15@student.university.edu' AS student_email, NULL AS admin_email, 'ACCOUNTS' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.55 AS sentiment_score, 0.84 AS confidence, '2026-03-16 12:00:00' AS created_at, '2026-03-19 12:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000040' AS ticket, 'Projector in Seminar Hall 2 not working (reported again)' AS title, 'The projector in Seminar Hall 2 has not worked for the last four sessions. Guest lectures are being conducted without slides.' AS descr, 'INFRASTRUCTURE' AS category, 'MEDIUM' AS priority, 'IN_PROGRESS' AS status, 'pooja.chatterjee16@student.university.edu' AS student_email, 'priya.menon@university.edu' AS admin_email, 'CIVIL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.44 AS sentiment_score, 0.77 AS confidence, '2026-06-19 11:00:00' AS created_at, '2026-06-26 11:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000041' AS ticket, 'Lab computers extremely slow in Block C (reported again)' AS title, 'The machines in the Block C programming lab take almost ten minutes to boot and the IDE freezes constantly. Half the two-hour slot is gone before anyone can start the actual exercise. This has been happening for three weeks now.' AS descr, 'IT_SUPPORT' AS category, 'HIGH' AS priority, 'OPEN' AS status, 'nikhil.sharma17@student.university.edu' AS student_email, NULL AS admin_email, 'IT' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.72 AS sentiment_score, 0.79 AS confidence, '2026-06-24 11:00:00' AS created_at, '2026-06-27 11:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000042' AS ticket, 'Mess food quality has dropped sharply this month (reported again)' AS title, 'The dinner served in the boys hostel mess has been repetitive and often undercooked since the vendor changed. Several students have reported stomach problems. Requesting an inspection of the kitchen.' AS descr, 'HOSTEL' AS category, 'URGENT' AS priority, 'REJECTED' AS status, 'divya.joshi18@student.university.edu' AS student_email, 'manoj.gupta@university.edu' AS admin_email, 'HOSTEL' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.85 AS sentiment_score, 0.74 AS confidence, '2026-04-27 06:00:00' AS created_at, '2026-04-28 06:00:00' AS due_at, NULL AS resolved_at, '2026-05-01 06:00:00' AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'Revaluation results for Semester 5 still not published (reported again)' AS title, 'It has been six weeks since revaluation applications closed for the Semester 5 Data Structures paper. The portal still shows the original marks and there is no announcement about when the revised results will appear.' AS descr, 'EXAMINATION' AS category, 'HIGH' AS priority, 'CLOSED' AS status, 'rahul.agarwal19@student.university.edu' AS student_email, 'sunita.rao@university.edu' AS admin_email, 'EXAM' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.58 AS sentiment_score, 0.92 AS confidence, '2026-03-24 17:00:00' AS created_at, '2026-03-27 17:00:00' AS due_at, '2026-03-26 06:00:00' AS resolved_at, '2026-03-30 06:00:00' AS closed_at, 3 AS rating
    UNION ALL
    SELECT 'FB-2026-000044' AS ticket, 'Library reading room closes too early during exams (reported again)' AS title, 'During the end-semester examination period the central reading room shuts at 8 PM. Many of us commute and cannot study at home. Extending the hours to 11 PM during exam weeks would help a lot.' AS descr, 'LIBRARY' AS category, 'MEDIUM' AS priority, 'RESOLVED' AS status, 'shreya.das20@student.university.edu' AS student_email, 'farah.khan@university.edu' AS admin_email, 'LIBRARY' AS dept, 0 AS anonymous, 'NEUTRAL' AS sentiment_label, -0.15 AS sentiment_score, 0.85 AS confidence, '2026-06-27 16:00:00' AS created_at, '2026-07-04 16:00:00' AS due_at, '2026-07-04 23:00:00' AS resolved_at, NULL AS closed_at, NULL AS rating
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'Excellent handling of the network outage last week (reported again)' AS title, 'I want to appreciate the IT team for restoring campus Wi-Fi within a few hours during the fibre cut last Tuesday. Updates were posted regularly and the temporary hotspot in the library was very helpful.' AS descr, 'IT_SUPPORT' AS category, 'LOW' AS priority, 'CLOSED' AS status, 'aryan.sinha21@student.university.edu' AS student_email, 'arjun.nair@university.edu' AS admin_email, 'IT' AS dept, 1 AS anonymous, 'POSITIVE' AS sentiment_label, 0.88 AS sentiment_score, 0.78 AS confidence, '2026-02-25 02:00:00' AS created_at, '2026-03-11 02:00:00' AS due_at, '2026-03-04 18:00:00' AS resolved_at, '2026-03-10 18:00:00' AS closed_at, 3 AS rating
    UNION ALL
    SELECT 'FB-2026-000046' AS ticket, 'Route 7 college bus consistently arrives 25 minutes late (reported again)' AS title, 'The 7:40 AM bus on Route 7 from Kondapur has been arriving between 8:05 and 8:15 every day this month. Students on this route are missing the first period regularly.' AS descr, 'TRANSPORT' AS category, 'HIGH' AS priority, 'IN_PROGRESS' AS status, 'kavya.reddy22@student.university.edu' AS student_email, 'vikram.bose@university.edu' AS admin_email, 'TRANSPORT' AS dept, 0 AS anonymous, 'NEGATIVE' AS sentiment_label, -0.66 AS sentiment_score, 0.74 AS confidence, '2026-05-27 10:00:00' AS created_at, '2026-05-30 10:00:00' AS due_at, NULL AS resolved_at, NULL AS closed_at, NULL AS rating
) AS x
JOIN users su ON su.email = x.student_email
JOIN students s ON s.user_id = su.id
JOIN departments d ON d.code = x.dept
LEFT JOIN users au ON au.email = x.admin_email
LEFT JOIN admins a ON a.user_id = au.id
WHERE NOT EXISTS (SELECT 1 FROM feedback f WHERE f.ticket_number = x.ticket);

INSERT INTO feedback_comments (feedback_id, author_id, body, internal_note, created_at)
SELECT f.id, u.id, x.body, x.internal_note, x.created_at FROM (
    SELECT 'FB-2026-000001' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-07-05 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000001' AS ticket, 'arjun.nair@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-07-07 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000001' AS ticket, 'arjun.nair@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-07-06 11:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000001' AS ticket, 'aarav.sharma1@student.university.edu' AS author_email, 'Confirming that the issue is resolved from my side as well. Appreciate it.' AS body, 0 AS internal_note, '2026-07-07 18:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000002' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-04-28 11:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000002' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-05-01 11:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000002' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'Completed and verified by our team today. Closing this unless you tell us otherwise.' AS body, 0 AS internal_note, '2026-05-01 16:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000002' AS ticket, 'ananya.joshi2@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-05-03 03:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'farah.khan@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-04-17 18:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'farah.khan@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-04-19 18:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'farah.khan@university.edu' AS author_email, 'Internal: vendor SLA expires in 48 hours, escalate if no response.' AS body, 1 AS internal_note, '2026-04-19 20:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'farah.khan@university.edu' AS author_email, 'Fixed. Thank you for your patience and for reporting it clearly.' AS body, 0 AS internal_note, '2026-04-28 01:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'sneha.das4@student.university.edu' AS author_email, 'Confirming that the issue is resolved from my side as well. Appreciate it.' AS body, 0 AS internal_note, '2026-04-29 03:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000005' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-07-09 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000005' AS ticket, 'arjun.nair@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-07-10 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'priya.menon@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-04-04 21:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'priya.menon@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-04-05 21:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'priya.menon@university.edu' AS author_email, 'Completed and verified by our team today. Closing this unless you tell us otherwise.' AS body, 0 AS internal_note, '2026-04-14 10:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'meera.verma8@student.university.edu' AS author_email, 'Thank you for the quick response.' AS body, 0 AS internal_note, '2026-04-15 15:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'priya.menon@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-07-10 09:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'priya.menon@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-07-13 09:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'priya.menon@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-07-18 09:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'aditya.mehta9@student.university.edu' AS author_email, 'Confirming that the issue is resolved from my side as well. Appreciate it.' AS body, 0 AS internal_note, '2026-07-19 12:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'farah.khan@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-03-09 06:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'farah.khan@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-03-12 06:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'farah.khan@university.edu' AS author_email, 'Internal: vendor SLA expires in 48 hours, escalate if no response.' AS body, 1 AS internal_note, '2026-03-12 08:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'farah.khan@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-03-19 16:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'nisha.fernandes10@student.university.edu' AS author_email, 'Thank you for the quick response.' AS body, 0 AS internal_note, '2026-03-19 22:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000011' AS ticket, 'rakesh.iyer@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-04-03 10:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000011' AS ticket, 'rakesh.iyer@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-04-05 10:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000011' AS ticket, 'rakesh.iyer@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-04-09 20:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000011' AS ticket, 'karan.kulkarni11@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-04-10 11:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000012' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-04-01 16:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000012' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-04-04 16:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'priya.menon@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-07-19 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'priya.menon@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-07-20 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'priya.menon@university.edu' AS author_email, 'Fixed. Thank you for your patience and for reporting it clearly.' AS body, 0 AS internal_note, '2026-07-21 15:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'tanvi.bhatt14@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-07-23 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000016' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-05-06 00:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000016' AS ticket, 'arjun.nair@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-05-07 00:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'priya.menon@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-05-20 03:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'priya.menon@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-05-21 03:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'priya.menon@university.edu' AS author_email, 'Internal: vendor SLA expires in 48 hours, escalate if no response.' AS body, 1 AS internal_note, '2026-05-21 05:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'priya.menon@university.edu' AS author_email, 'Completed and verified by our team today. Closing this unless you tell us otherwise.' AS body, 0 AS internal_note, '2026-05-23 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'nikhil.sharma17@student.university.edu' AS author_email, 'Confirming that the issue is resolved from my side as well. Appreciate it.' AS body, 0 AS internal_note, '2026-05-24 06:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000018' AS ticket, 'farah.khan@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-03-20 15:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000018' AS ticket, 'farah.khan@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-03-23 15:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000019' AS ticket, 'deepa.singh@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-07-01 01:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000019' AS ticket, 'deepa.singh@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-07-04 01:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000019' AS ticket, 'deepa.singh@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-07-04 15:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000019' AS ticket, 'rahul.agarwal19@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-07-05 05:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000020' AS ticket, 'priya.menon@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-05-17 02:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000020' AS ticket, 'priya.menon@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-05-18 02:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000020' AS ticket, 'priya.menon@university.edu' AS author_email, 'Internal: vendor SLA expires in 48 hours, escalate if no response.' AS body, 1 AS internal_note, '2026-05-18 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-02-27 21:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'arjun.nair@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-03-02 21:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Fixed. Thank you for your patience and for reporting it clearly.' AS body, 0 AS internal_note, '2026-03-04 20:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'aryan.sinha21@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-03-05 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-04-30 23:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-05-02 23:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-05-05 12:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'kavya.reddy22@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-05-06 09:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000023' AS ticket, 'sunita.rao@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-05-29 18:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000023' AS ticket, 'sunita.rao@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-06-01 18:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000024' AS ticket, 'farah.khan@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-07-13 10:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000024' AS ticket, 'farah.khan@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-07-16 10:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000024' AS ticket, 'farah.khan@university.edu' AS author_email, 'Fixed. Thank you for your patience and for reporting it clearly.' AS body, 0 AS internal_note, '2026-07-22 18:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000024' AS ticket, 'neha.verma24@student.university.edu' AS author_email, 'Confirming that the issue is resolved from my side as well. Appreciate it.' AS body, 0 AS internal_note, '2026-07-23 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-03-12 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'arjun.nair@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-03-13 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Internal: vendor SLA expires in 48 hours, escalate if no response.' AS body, 1 AS internal_note, '2026-03-13 15:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'arjun.nair@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-03-21 16:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'aarav.sharma1@student.university.edu' AS author_email, 'Confirming that the issue is resolved from my side as well. Appreciate it.' AS body, 0 AS internal_note, '2026-03-22 20:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000028' AS ticket, 'priya.menon@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-04-27 01:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000028' AS ticket, 'priya.menon@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-04-28 01:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000029' AS ticket, 'priya.menon@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-07-11 20:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000029' AS ticket, 'priya.menon@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-07-13 20:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'farah.khan@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-07-19 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'farah.khan@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-07-20 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'farah.khan@university.edu' AS author_email, 'Completed and verified by our team today. Closing this unless you tell us otherwise.' AS body, 0 AS internal_note, '2026-07-27 18:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'ishita.reddy6@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-07-29 08:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'rakesh.iyer@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-06-22 14:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'rakesh.iyer@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-06-25 14:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'rakesh.iyer@university.edu' AS author_email, 'Completed and verified by our team today. Closing this unless you tell us otherwise.' AS body, 0 AS internal_note, '2026-07-06 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'vivaan.pillai7@student.university.edu' AS author_email, 'Confirming that the issue is resolved from my side as well. Appreciate it.' AS body, 0 AS internal_note, '2026-07-07 23:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000032' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-05-23 21:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000032' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-05-25 21:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000033' AS ticket, 'sunita.rao@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-04-04 00:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000033' AS ticket, 'sunita.rao@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-04-05 00:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'priya.menon@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-03-05 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'priya.menon@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-03-06 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'priya.menon@university.edu' AS author_email, 'Fixed. Thank you for your patience and for reporting it clearly.' AS body, 0 AS internal_note, '2026-03-06 03:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'nisha.fernandes10@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-03-06 10:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-06-12 02:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'arjun.nair@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-06-14 02:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Fixed. Thank you for your patience and for reporting it clearly.' AS body, 0 AS internal_note, '2026-06-15 17:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'riya.kaur12@student.university.edu' AS author_email, 'Thank you for the quick response.' AS body, 0 AS internal_note, '2026-06-16 23:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000037' AS ticket, 'priya.menon@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-04-04 22:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'farah.khan@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-04-04 02:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'farah.khan@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-04-07 02:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'farah.khan@university.edu' AS author_email, 'Internal: vendor SLA expires in 48 hours, escalate if no response.' AS body, 1 AS internal_note, '2026-04-07 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'farah.khan@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-04-09 23:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'tanvi.bhatt14@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-04-11 12:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000040' AS ticket, 'priya.menon@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-06-20 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000040' AS ticket, 'priya.menon@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-06-22 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000040' AS ticket, 'priya.menon@university.edu' AS author_email, 'Internal: vendor SLA expires in 48 hours, escalate if no response.' AS body, 1 AS internal_note, '2026-06-22 06:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000042' AS ticket, 'manoj.gupta@university.edu' AS author_email, 'Received. We are looking into this and will get back to you shortly.' AS body, 0 AS internal_note, '2026-04-27 19:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'sunita.rao@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-03-25 03:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'sunita.rao@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-03-27 03:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'sunita.rao@university.edu' AS author_email, 'Fixed. Thank you for your patience and for reporting it clearly.' AS body, 0 AS internal_note, '2026-03-26 05:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'rahul.agarwal19@student.university.edu' AS author_email, 'Thank you for the quick response.' AS body, 0 AS internal_note, '2026-03-27 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000044' AS ticket, 'farah.khan@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-06-28 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000044' AS ticket, 'farah.khan@university.edu' AS author_email, 'We have escalated this to the vendor. A response is expected by the end of this week.' AS body, 0 AS internal_note, '2026-06-29 04:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000044' AS ticket, 'farah.khan@university.edu' AS author_email, 'This has now been resolved. Please verify at your convenience and let us know if the problem recurs.' AS body, 0 AS internal_note, '2026-07-04 22:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000044' AS ticket, 'shreya.das20@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-07-06 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Noted, thanks for the detail in your report - it makes this much easier to act on.' AS body, 0 AS internal_note, '2026-02-25 06:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'arjun.nair@university.edu' AS author_email, 'The maintenance team inspected the site this morning. Parts have been ordered and work should begin on Monday.' AS body, 0 AS internal_note, '2026-02-26 06:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'arjun.nair@university.edu' AS author_email, 'Completed and verified by our team today. Closing this unless you tell us otherwise.' AS body, 0 AS internal_note, '2026-03-04 17:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'aryan.sinha21@student.university.edu' AS author_email, 'Any update on this? It has been a few days.' AS body, 0 AS internal_note, '2026-03-06 00:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000046' AS ticket, 'vikram.bose@university.edu' AS author_email, 'Thank you for raising this. I have logged it with the concerned team and will update you within two working days.' AS body, 0 AS internal_note, '2026-05-27 13:00:00' AS created_at
    UNION ALL
    SELECT 'FB-2026-000046' AS ticket, 'vikram.bose@university.edu' AS author_email, 'A technician has been assigned. Please bear with us while the replacement arrives.' AS body, 0 AS internal_note, '2026-05-30 13:00:00' AS created_at
) AS x
JOIN feedback f ON f.ticket_number = x.ticket
JOIN users u ON u.email = x.author_email
WHERE NOT EXISTS (
    SELECT 1 FROM feedback_comments c WHERE c.feedback_id = f.id AND c.created_at = x.created_at
);

INSERT INTO feedback_status_history (feedback_id, from_status, to_status, changed_by_id, note, changed_at)
SELECT f.id, x.from_status, x.to_status, u.id, x.note, x.changed_at FROM (
    SELECT 'FB-2026-000001' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-07-05 19:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000001' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-06 12:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000001' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-07-09 12:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000002' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-29 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000002' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-05-01 17:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-17 10:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-04-28 02:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000004' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-05-02 02:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000005' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-07-09 17:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-05 18:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-04-14 11:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000008' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-04-18 11:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-07-09 10:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-18 10:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000009' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-07-19 10:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-03-09 14:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000010' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-03-19 17:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000011' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'rakesh.iyer@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-02 11:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000011' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'rakesh.iyer@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-04-09 21:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000012' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-02 07:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-07-20 09:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-21 16:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000014' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-07-25 16:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000016' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-05-06 02:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-05-20 10:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-05-23 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000017' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-05-27 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000018' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-03-20 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000018' AS ticket, 'IN_PROGRESS' AS from_status, 'AWAITING_STUDENT' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Waiting for additional details from the student.' AS note, '2026-03-21 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000019' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'deepa.singh@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-06-30 22:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000019' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'deepa.singh@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-04 16:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000020' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-05-17 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000020' AS ticket, 'IN_PROGRESS' AS from_status, 'AWAITING_STUDENT' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Waiting for additional details from the student.' AS note, '2026-05-18 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-02-28 02:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-03-04 21:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000021' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-03-10 21:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-30 20:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-05-05 13:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000022' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-05-10 13:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000023' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'sunita.rao@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-05-30 04:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000024' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-07-13 02:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000024' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-22 19:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-03-12 02:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000025' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-03-21 17:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000028' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-26 22:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000029' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-07-11 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-07-18 13:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-27 19:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000030' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-08-02 19:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'rakesh.iyer@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-06-23 00:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'rakesh.iyer@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-06 14:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000031' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'rakesh.iyer@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-07-07 14:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000032' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-05-24 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000032' AS ticket, 'IN_PROGRESS' AS from_status, 'AWAITING_STUDENT' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Waiting for additional details from the student.' AS note, '2026-05-25 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000033' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'sunita.rao@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-04 05:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-03-04 23:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-03-06 04:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000034' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-03-11 04:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-06-11 08:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-06-15 18:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000036' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-06-19 18:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000037' AS ticket, 'OPEN' AS from_status, 'REJECTED' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Duplicate of an existing ticket.' AS note, '2026-04-05 17:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-04-04 12:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000038' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-04-10 00:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000040' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'priya.menon@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-06-19 18:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000042' AS ticket, 'OPEN' AS from_status, 'REJECTED' AS to_status, 'manoj.gupta@university.edu' AS actor_email, 'Duplicate of an existing ticket.' AS note, '2026-04-27 22:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'sunita.rao@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-03-25 02:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'sunita.rao@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-03-26 06:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000043' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'sunita.rao@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-03-30 06:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000044' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-06-28 08:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000044' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'farah.khan@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-07-04 23:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-02-25 04:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'IN_PROGRESS' AS from_status, 'RESOLVED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Resolution communicated to the student.' AS note, '2026-03-04 18:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000045' AS ticket, 'RESOLVED' AS from_status, 'CLOSED' AS to_status, 'arjun.nair@university.edu' AS actor_email, 'Confirmed by the student.' AS note, '2026-03-10 18:00:00' AS changed_at
    UNION ALL
    SELECT 'FB-2026-000046' AS ticket, 'OPEN' AS from_status, 'IN_PROGRESS' AS to_status, 'vikram.bose@university.edu' AS actor_email, 'Picked up by the department.' AS note, '2026-05-27 15:00:00' AS changed_at
) AS x
JOIN feedback f ON f.ticket_number = x.ticket
JOIN users u ON u.email = x.actor_email
WHERE NOT EXISTS (
    SELECT 1 FROM feedback_status_history h WHERE h.feedback_id = f.id AND h.changed_at = x.changed_at
);

