function intAdd(int a, int b) returns (int) {
    return a + b;
}

function floatAdd(float a, float b) returns (float) {
    return a + b;
}

function stringAdd(string a, string b) returns (string) {
    return a + b;
}

function stringAndIntAdd(string a, int b) returns (string) {
    return a + b.toString();
}

function xmlXmlAdd() returns (xml) {
    xml a = xml `abc`;
    xml b = xml `def`;
    return a + b;
}

function xmlStringAdd() returns (xml) {
    xml a = xml `abc`;
    string b = "def";
    return a + b;
}

function stringXmlAdd() returns (xml) {
    string a = "def";
    xml b = xml `abc`;
    return a + b;
}

public const A = 10;
public const B = 20;
public const C = 30;
public const D = 40;

type SomeTypes A|B|C|D;

type E 12|13|14;

const float F = 20.25;
const float G = 10.5;

type H F|G;

type I 10.5|30.4;

const decimal J = 4.565;
const decimal K = 10.5;

type L J|K;

const M = "M";
const N = "N";

type O M|N;

type P "Cat"|"Dog";

function testAdditionWithTypes() {
    SomeTypes a1 = 10;
    int a2 = 20;
    SomeTypes a3 = 30;
    byte a4 = 25;
    int|int:Signed16 a5 = 15;
    E a6 = 12;
    float a7 = 10.5;
    H a8 = 10.5;
    I a9 = 30.4;
    L a10 = 10.5;
    decimal a11 = 20.5;
    decimal a12 = 30.5;
    byte a13 = 15;

    assertEqual(a1 + a2, 30);
    assertEqual(a2 + a1, 30);
    assertEqual(a2 + a3, 50);
    assertEqual(a3 + a4, 55);
    assertEqual(a1 + a5, 25);
    assertEqual(a1 + a6, 22);
    assertEqual(a4 + a5, 40);
    assertEqual(a4 + a6, 37);
    assertEqual(a5 + a6, 27);
    assertEqual(a7 + a8, 21.0);
    assertEqual(a7 + a9, 40.9);
    assertEqual(a8 + a9, 40.9);
    assertEqual(a10 + a11, 31d);
    assertEqual(a11 + a12, 51d);
    assertEqual(a4 + a13, 40);

    string a14 = "abc";
    O a15 = "M";
    string|string:Char a16 = "EFG";
    O a17 = "N";
    P a18 = "Cat";
    xml a19 = xml `abc`;
    xml:Text|xml a20 = xml `abdef`;

    assertEqual(a14 + a15, "abcM");
    assertEqual(a15 + a16, "MEFG");
    assertEqual(a15 + a17, "MN");
    assertEqual(a15 + a1.toString(), "M10");
    assertEqual(a15 + a18, "MCat");
    assertEqual(a15 + a19, xml `Mabc`);
    assertEqual(a19 + a15, xml `abcM`);
    assertEqual(a16 + a19, xml `EFGabc`);
    assertEqual(a17 + a19, xml `Nabc`);
    assertEqual(a18 + a19, xml `Catabc`);
    assertEqual(a19 + a20, xml `abcabdef`);
    assertEqual(a20 + a19, xml `abdefabc`);
}

function testAddSingleton() {
    1 a1 = 1;
    int a2 = 2;
    20.5 a3 = 20.5;
    float a4 = 10.5;
    SomeTypes a5 = 10;
    int|int:Signed16 a6 = 15;
    E a7 = 12;

    assertEqual(a1 + a2, 3);
    assertEqual(a3 + a4, 31.0);
    assertEqual(a1 + a5, 11);
    assertEqual(a1 + a6, 16);
    assertEqual(a1 + a7, 13);
}

function assertEqual(any actual, any expected) {
    if actual is anydata && expected is anydata && actual == expected {
        return;
    }

    if actual === expected {
        return;
    }

    string actualValAsString = actual.toString();
    string expectedValAsString = expected.toString();
    panic error(string `Assertion error: expected ${expectedValAsString} found ${actualValAsString}`);
}
