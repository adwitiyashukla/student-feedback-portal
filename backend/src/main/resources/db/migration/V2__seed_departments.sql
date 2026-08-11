INSERT INTO departments (code, name, description, active, created_at, updated_at)
VALUES
    ('CSE',       'Computer Science & Engineering', 'Courses, labs and faculty for CSE',        TRUE, NOW(6), NOW(6)),
    ('ECE',       'Electronics & Communication',    'Courses, labs and faculty for ECE',        TRUE, NOW(6), NOW(6)),
    ('MECH',      'Mechanical Engineering',         'Courses, workshops and faculty for MECH',  TRUE, NOW(6), NOW(6)),
    ('CIVIL',     'Civil Engineering',              'Courses, site work and faculty for Civil', TRUE, NOW(6), NOW(6)),
    ('EXAM',      'Examination Cell',               'Results, revaluation and exam scheduling', TRUE, NOW(6), NOW(6)),
    ('HOSTEL',    'Hostel Administration',          'Rooms, mess, warden and hostel facilities',TRUE, NOW(6), NOW(6)),
    ('LIBRARY',   'Central Library',                'Books, journals and reading rooms',        TRUE, NOW(6), NOW(6)),
    ('TRANSPORT', 'Transport Office',               'Bus routes, passes and timings',           TRUE, NOW(6), NOW(6)),
    ('ACCOUNTS',  'Accounts & Fees',                'Fee payment, refunds and scholarships',    TRUE, NOW(6), NOW(6)),
    ('IT',        'IT Services',                    'Campus network, Wi-Fi and portal accounts',TRUE, NOW(6), NOW(6))
AS new
ON DUPLICATE KEY UPDATE
    name        = new.name,
    description = new.description,
    updated_at  = NOW(6);
