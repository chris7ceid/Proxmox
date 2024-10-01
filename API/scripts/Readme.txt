Run docker compose
check swagger doc http://localhost:8080/swagger-ui/index.html
Register and sign in a user through api.
Then add the admin role manually for security reasons
Go to docker postgres terminal
psql -U ceid -d  iris_api
iris_api=# select * from roles;
 id |      name      
----+----------------
  1 | ROLE_USER
  2 | ROLE_ADMIN
  3 | ROLE_MODERATOR
(3 rows)
insert into user_roles(user_id,role_id) values (1,2);