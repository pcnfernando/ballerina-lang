import ballerina/io;
import ballerina/jdbc;
import ballerina/sql;

// Client endpoint for MySQL database. This client endpoint can be used with any jdbc
// supported database by providing the corresponding jdbc url.
endpoint jdbc:Client testDB {
    url: "jdbc:mysql://localhost:3306/testdb",
    username: "root",
    password: "root",
    poolOptions: { maximumPoolSize: 5 },
    dbOptions: { useSSL: false }
};

// This is the type created to represent data row.
type Student {
    int id,
    int age,
    string name,
};

function main(string... args) {
    // Creates a table using the update action. If the DDL
    // statement execution is successful, the `update` action returns 0.
    io:println("The update operation - Creating a table:");
    var ret = testDB->update("CREATE TABLE student(id INT AUTO_INCREMENT,
                              age INT, name VARCHAR(255), PRIMARY KEY (id))");
    handleUpdate(ret, "Create student table");

    // Inserts data to the table using the update action. If the DML statement execution
    // is successful, the `update` action returns the updated row count.
    // The query parameters are given in the query statement it self.
    io:println("\nThe update operation - Inserting data to a table");
    ret = testDB->update("INSERT INTO student(age, name) values (23, 'john')");
    handleUpdate(ret, "Insert to student table with no parameters");

    // The query parameters are given as variables for the update operation. Only int,
    // float, boolean, string and blob values are supported as direct variables.
    int age = 24;
    string name = "Anne";
    ret = testDB->update("INSERT INTO student(age, name) values (?, ?)", age, name);
    handleUpdate(ret, "Insert to student table with variable parameters");

    // The query parameters are given as sql:Parameters for the update operation.
    // Default direction is IN.
    sql:Parameter p1 = { sqlType: sql:TYPE_INTEGER, value: 25 };
    sql:Parameter p2 = { sqlType: sql:TYPE_VARCHAR, value: "James" };
    ret = testDB->update("INSERT INTO student(age, name) values (?, ?)", p1, p2);
    handleUpdate(ret, "Insert to student table with sql:parameter values");


    // Update data in the table using the update action.
    io:println("\nThe Update operation - Update data in a table");
    ret = testDB->update("Update student set name = 'Johnes' where age = ?", 23);
    handleUpdate(ret, "Update a row in student table");

    // Delete data in a table using the update action.
    io:println("\nThe Update operation - Delete data from table");
    ret = testDB->update("DELETE FROM student where age = ?", 24);
    handleUpdate(ret, "Delete a row from student table");

    // Column values generated during the update can be retrieved using the
    // `updateWithGeneratedKeys` action. If the table has several auto
    // generated columns other than the auto incremented key, those column
    // names should be given as an array. The values of the auto incremented
    // column and the auto generated columns are returned as a string array.
    // Similar to the `update` action, the inserted row count is also returned.
    io:println("\nThe updateWithGeneratedKeys operation - Inserting data to a table");
    age = 31;
    name = "Kate";
    var retWithKey = testDB->updateWithGeneratedKeys("INSERT INTO student(age, name)
                            values (?, ?)", (), age, name);
    match retWithKey {
        (int, string[]) y => {
            var (count, ids) = y;
            io:println("Inserted row count: " + count);
            io:println("Generated key: " + ids[0]);
        }
        error err => io:println("Insert to student table failed: " + err.message);
    }

    // A batch of data can be inserted using the `batchUpdate` action. The number
    // of inserted rows for each insert in the batch is returned as an array.
    io:println("\nThe batchUpdate operation - Inserting a batch of data to a table");
    sql:Parameter para1 = { sqlType: sql:TYPE_INTEGER, value: 27 };
    sql:Parameter para2 = { sqlType: sql:TYPE_VARCHAR, value: "Alex" };
    sql:Parameter[] parameters1 = [para1, para2];

    //Create the second batch of parameters.
    sql:Parameter para3 = { sqlType: sql:TYPE_INTEGER, value: 28 };
    sql:Parameter para4 = { sqlType: sql:TYPE_VARCHAR, value: "Peter" };
    sql:Parameter[] parameters2 = [para3, para4];

    //Do the batch update by passing the multiple parameter arrays.
    var retBatch = testDB->batchUpdate("INSERT INTO student(age, name) values (?, ?)",
        parameters1, parameters2);
    match retBatch {
        int[] counts => {
            io:println("Batch 1 update counts: " + counts[0]);
            io:println("Batch 2 update counts: " + counts[1]);
        }
        error err => io:println("Batch update action failed: " + err.message);
    }

    // Call operiation is used to invoke a stored procedure. Here stored procedure
    // with IN parameters is invoked.
    io:println("\nThe call operation - Calling a stored procedure with IN params");
    // Create the stored procedure
    ret = testDB->update("CREATE PROCEDURE INSERTDATA(IN pAge INT, IN pName VARCHAR(255))
							   BEGIN
							      INSERT INTO student(age, name) values (pAge, pName);
							   END");
    handleUpdate(ret, "Stored procedure with IN param creation");

    // Invoke the stored procedure with IN type parameters.
    var retCall = testDB->call("{CALL INSERTDATA(?,?)}", (), 19, "George");
    match retCall {
        ()|table[] => io:println("Call action with IN params successful");
        error err => io:println("Stored procedure call failed: " + err.message);
    }

    // Here stored procedure with OUT and INOUT parameters is invoked.
    io:println("\nThe call operation - Calling a stored procedure with INOUT/OUT params");
    // Create the stored procedure.
    ret = testDB->update("CREATE PROCEDURE GETCOUNT (INOUT pID INT, OUT pCount INT)
                               BEGIN
                                  SELECT id INTO pID FROM student WHERE age = pID;
                                  SELECT COUNT(*) INTO pCount FROM student WHERE age = 27;
                               END");
    handleUpdate(ret, "Stored procedure with INOUT/OUT param creation");

    // Inovke the stored procedure.
    sql:Parameter param1 = { sqlType: sql:TYPE_INTEGER, value: 25,
        direction: sql:DIRECTION_INOUT };
    sql:Parameter param2 = { sqlType: sql:TYPE_INTEGER, direction: sql:DIRECTION_OUT };
    retCall = testDB->call("{CALL GETCOUNT(?,?)}", (), param1, param2);
    match retCall {
        ()|table[] => {
            io:println("Call action with INOUT and OUT params successful");
            io:print("Student ID of the person with age = 25: ");
            io:println(param1.value);
            io:print("Student count with age = 27: ");
            io:println(param2.value);
        }
        error err => io:println("Stored procedure call failed: " + err.message);
    }

    // A proxy for a database table that allows performing add/remove operations over
    // the actual database table, can be obtained by `getProxyTable` action.
    io:println("\nThe getProxyTable operation - Get a proxy for a table and view data");
    var proxyRet = testDB->getProxyTable("student", Student);
    table<Student> tbProxy;
    match proxyRet {
        table tbReturned => tbProxy = tbReturned;
        error err => io:println("Proxy table retrieval failed: " + err.message);
    }

    // Iterate through the table and retrieve the data record corresponding to each row.
    foreach record in tbProxy {
        io:println("Student:" + record.id + "|" + record.name + "|" + record.age);
    }

    // Data can be added to the database table through the proxied table.
    io:println("\nAdd data to a proxied table");
    Student s = { name: "Tim", age: 14 };
    var addRet = tbProxy.add(s);
    match addRet {
        () => io:println("Insertion to table successful");
        error err => io:println("Insertion to table failed: " + err.message);
    }

    // Data can be removed from the database table through the proxied table, by passing
    // a function pointer which returns a boolean value evaluating whether a given record
    // should be removed or not.
    io:println("\nRemove data from a proxied table");
    var rmRet = tbProxy.remove(isUnder20);
    match rmRet {
        int count => io:println("Removed count: " + count);
        error err => io:println("Removing from table failed: " + err.message);
    }

    // Select data using the `select` action. The `select` action returns a table.
    // See the `table` ballerina example for more details on how to access data.
    io:println("\nThe select operation - Select data from a database table");
    var selectRet = testDB->select("SELECT * FROM student", Student);
    table<Student> dt;
    match selectRet {
        table tableReturned => dt = tableReturned;
        error err => io:println("Select data from student table failed: " + err.message);
    }
    // Conversion from type 'table' to either JSON or XML results in data streaming.
    // When a service client makes a request, the result is streamed to the service
    // client rather than building the full result in the server and returning it.
    // This allows unlimited payload sizes in the result and the response is
    // instantaneous to the client.
    // Convert a table to JSON.
    var jsonConversionRet = <json>dt;
    match jsonConversionRet {
        json jsonRes => {
            io:print("JSON: ");
            io:println(io:sprintf("%s", jsonRes));
        }
        error e => io:println("Error in table to json conversion");
    }

    // Re-iteration of the result is possible only if `loadToMemory` named argument
    // is set to `true` in `select` action.
    io:println("\nThe select operation - Select data by loading table to memory");
    selectRet = testDB->select("SELECT * FROM student", Student, loadToMemory = true);
    match selectRet {
        table tableReturned => dt = tableReturned;
        error err => io:println("Select data from student table failed: " + err.message);
    }

    // Iterating data first time.
    io:println("Iterating data first time:");
    foreach record in dt {
        io:println("Student:" + record.id + "|" + record.name + "|" + record.age);
    }
    // Iterating data second time.
    io:println("Iterating data second time:");
    foreach record in dt {
        io:println("Student:" + record.id + "|" + record.name + "|" + record.age);
    }

    //Drop the table and procedures.
    io:println("\nThe update operation - Drop the created tables and procedures");
    ret = testDB->update("DROP TABLE student");
    handleUpdate(ret, "Drop table student");

    ret = testDB->update("DROP PROCEDURE INSERTDATA");
    handleUpdate(ret, "Drop stored procedure INSERTDATA");

    ret = testDB->update("DROP PROCEDURE GETCOUNT");
    handleUpdate(ret, "Drop stored procedure GETCOUNT");

    // Finally, close the connection pool.
    testDB.stop();
}

// Check crieteria for remove.
function isUnder20(Student s) returns boolean {
    return s.age < 20;
}

// Function to handle return of the update operation.
function handleUpdate(int|error returned, string message) {
    match returned {
        int retInt => io:println(message + " status: " + retInt);
        error err => io:println(message + " failed: " + err.message);
    }
}
