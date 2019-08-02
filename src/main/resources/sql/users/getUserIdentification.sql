SELECT hashed_password,
       salt
FROM users
WHERE user_name = ?