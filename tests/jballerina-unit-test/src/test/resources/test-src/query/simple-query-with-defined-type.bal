// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
type Person record {
    string name;
    int age;
};

type Employee record {
    string name;
    string company;
};

//function testFilterFunc() returns boolean {
//    boolean testPassed = true;
//    Person[] personList = getPersonList();
//
//    stream<Person> personStream = personList.toStream();
//    stream<Person> filteredPersonStream = personStream.filter(function (Person person) returns boolean {
//        return person.age > 100 && person.name != "James";
//    });
//
//    record {|Person value;|}? filteredPerson = filteredPersonStream.next();
//    testPassed = testPassed && filteredPerson?.value == personList[1];
//
//    filteredPerson = filteredPersonStream.next();
//    testPassed = testPassed && filteredPerson?.value == personList[2];
//
//    filteredPerson = filteredPersonStream.next();
//    testPassed = testPassed && filteredPerson?.value == personList[4];
//
//    filteredPerson = filteredPersonStream.next();
//    testPassed = testPassed && filteredPerson == ();
//
//    return testPassed;
//}
//
//function testMapFunc() returns boolean {
//    boolean testPassed = true;
//    Person[] personList = getPersonList();
//
//    stream<Person> personStream = personList.toStream();
//    stream<Employee> employeeStream = personStream.'map(function (Person person) returns Employee {
//        Employee e = {
//            name: person.name,
//            company: "WSO2"
//        };
//        return e;
//    });
//
//    var employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Gima" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Mohan" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Grainier" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Chiran" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Sinthuja" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee == ();
//
//    return testPassed;
//}
//
//function testFilterAndMapFunc() returns boolean {
//    boolean testPassed = true;
//    Person[] personList = getPersonList();
//
//    stream<Person> personStream = personList.toStream();
//    stream<Employee> employeeStream = personStream
//    . filter(function (Person person) returns boolean {
//        return person.age > 100 && person.name != "James";
//    }
//    )
//    . 'map(function (Person person) returns Employee {
//        Employee e = {
//            name: person.name,
//            company: "WSO2"
//        };
//        return e;
//    }
//    );
//
//    var employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Mohan" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Grainier" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee?.value?.name == "Sinthuja" && employee?.value?.company == "WSO2";
//
//    employee = employeeStream.next();
//    testPassed = testPassed && employee == ();
//
//    return testPassed;
//}
//
//function testReduce() returns float {
//    Person[] personList = getPersonList();
//    stream<Person> personStream = personList.toStream();
//    float avg = personStream.reduce(function (float accum, Person person) returns float {
//        return accum + <float>person.age / personList.length();
//    }, 0.0);
//    return avg;
//}
//
//function testForEach() returns Person[] {
//    Person chiran = {name: "Chiran", age: 75};
//    Person[] personList = [chiran];
//
//    Person[] filteredList = [];
//    stream<Person> streamedPerson = personList.toStream();
//    streamedPerson.forEach(function (Person person) {
//       //if(person.age == 75) {
//           filteredList[filteredList.length()] = person;
//       //}
//    });
//    io:println(filteredList);
//    return filteredList;
//}

function testQuery() returns int[]{
     Person chiran = {name: "Chiran", age: 75};
     Person[] personList = [chiran];

    int[] filteredList = from Person person in personList
            //where person.age == 75
            select 12;
            io:println(filteredList);
    return  filteredList;
}

//function testIterator() returns boolean {
//    boolean testPassed = true;
//    Person[] personList = getPersonList();
//
//    stream<Person> personStream = personList.toStream();
//    var iterator = personStream.iterator();
//
//    record {|Person value;|}? filteredPerson = iterator.next();
//    testPassed = testPassed && filteredPerson?.value == personList[0];
//
//    filteredPerson = iterator.next();
//    testPassed = testPassed && filteredPerson?.value == personList[1];
//
//    filteredPerson = iterator.next();
//    testPassed = testPassed && filteredPerson?.value == personList[2];
//
//    filteredPerson = iterator.next();
//    testPassed = testPassed && filteredPerson?.value == personList[3];
//
//    filteredPerson = iterator.next();
//    testPassed = testPassed && filteredPerson?.value == personList[4];
//
//    filteredPerson = iterator.next();
//    testPassed = testPassed && filteredPerson == ();
//
//    return testPassed;
//}
