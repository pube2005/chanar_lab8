# Лаборатори 8 Mockito mock объектууд

Оюутны нэр: **П.Пүрэвдаш**  
Оюутны код: **B232270144**  
Хичээл: **F.CSA313 Программ хангамжийн чанарын баталгаа ба тест**

`CourseRegistrationService.register()` үйлчилгээг бодит өгөгдлийн сан, SMTP сервергүйгээр JUnit 5 ба Mockito 5 ашиглан тусгаарлаж тестлэв. A1–A8 нь нийт **10 тестийн ажиллуулалттай**: 7 энгийн тест, A8 параметржүүлсэн тестийн 3 мөр. Гурван мутаци бүгд тест унагаасан бөгөөд эцэст нь `src/main`-ийг starter хувилбарт бүрэн сэргээсэн.

## Ажиллуулалтын орчин

`java -version` командын гаралт:

```text
openjdk version "21.0.12.1" 2026-08-18
OpenJDK Runtime Environment (build 21.0.12.1+1-1-deb13u1-Debian)
OpenJDK 64-Bit Server VM (build 21.0.12.1+1-1-deb13u1-Debian, mixed mode, sharing)
```

`mvn -version` командын гаралт (өнгөний ANSI тэмдэглэгээг хассан):

```text
Apache Maven 3.9.9
Maven home: /usr/share/maven
Java version: 21.0.12.1, vendor: Debian, runtime: /usr/lib/jvm/java-21-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "6.12.113+deb13-amd64", arch: "amd64", family: "unix"
```

`pom.xml` нь Java release 17, JUnit Jupiter 5.10.2, Mockito JUnit Jupiter 5.11.0, Surefire 3.2.5 ашиглана. JDK 21 дээрх Byte Buddy agent-ийн анхааруулга тестийн алдаа биш; бодит гаралтад нь хэвээр хадгалсан.

## Төслийн бүтэц

```text
.
├── .gitignore
├── pom.xml
├── README.md
├── src/main/java/mn/edu/must/sqat/
│   ├── CourseRegistrationService.java
│   ├── EmailSender.java
│   ├── Student.java
│   └── StudentRepository.java
├── src/test/java/mn/edu/must/sqat/
│   └── CourseRegistrationServiceTest.java
└── results/
    ├── mvn-test.txt
    ├── mvn-test-mutant-1.txt
    ├── mvn-test-mutant-2.txt
    ├── mvn-test-mutant-3.txt
    └── clone-test.txt
```

`target/`, `.idea/`, `.DS_Store`, зааврын `.docx`, starter задлалтын үлдэгдэл болон `.verification/` локал шалгалтын хавтас Git-д орохгүй.

## Хэсэг A Тестүүд

Бүх тест `@ExtendWith(MockitoExtension.class)` ашиглана. Хамаарлуудыг `@Mock`-оор үүсгэж, тест бүрийн өмнө constructor injection-оор үйлчилгээнд дамжуулна. `Student` ба хичээлийн жагсаалтууд нь бодит утгын объектууд.

| Тест | Методын нэр | Шалгах үр дүн ба хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudentWithPrerequisites_returnsOkAndSavesAndEmails` | Өмнөх холбоо хичээлүүдээ үзсэн идэвхтэй оюутанд `OK`; `when`, `InOrder.verify` ашиглан хадгалалт, дараа нь зөв хаяг, гарчиг, биетэй мэдэгдэл шалгана. |
| A2 | `register_missingStudent_returnsNoStudentWithoutSavingOrEmailing` | `ERROR_NO_STUDENT`; `verify(..., never())` ашиглан хадгалалт, и-мэйл яваагүйг шалгана. |
| A3 | `register_inactiveStudent_returnsInactiveWithoutEmailInteractions` | `ERROR_INACTIVE_STUDENT`; хадгалаагүй, `verifyNoInteractions(emailSender)`. |
| A4 | `register_missingPrerequisite_returnsPrerequisitesWithoutSaving` | Хоёр prerequisite-ийн нэг нь дутуу үед `ERROR_PREREQUISITES`; хадгалалт, и-мэйлгүй. |
| A5 | `register_emailSenderThrows_returnsOkAndKeepsRegistration` | `doThrow`-оор SMTP алдаа дуурайлгахад `OK`; хадгалалт болон и-мэйл илгээх оролдлого тус бүр нэг удаа. |
| A6 | `register_eitherOrBothIdsNull_throwsWithoutDependencyInteractions` | `studentId=null`, `courseId=null`, хоёулаа `null` гэсэн 3 нөхцөлд `assertThrows`; хамаарлуудтай огт харьцаагүй. Нэг `@Test` дотор `assertAll` хэрэглэсэн. |
| A7 | `register_successfulRegistration_emailBodyContainsCourseCode` | `ArgumentCaptor<String>`-оор и-мэйлийн биеийг барьж, хичээлийн код болон бүтэн агуулгыг шалгана. |
| A8 | `register_invalidStudentOrCourse_returnsExpectedErrorWithoutSideEffects` | `@ParameterizedTest` + `@CsvSource`: байхгүй оюутан, идэвхгүй оюутан, байхгүй хичээл гэсэн 3 алдааны ангийг шалгана. Бүгд хадгалалт, и-мэйлгүй. |

A8-д эрт буцах салаанд ашиглагдахгүй stub-уудыг нийтлэгээр тохируулбал strict stubs горим `UnnecessaryStubbingException` өгч болох бөгөөд `lenient()` нь тухайн stub-ийн энэ шалгалтыг сулруулдаг. Энэ хэрэгжүүлэлт мөр бүрийн салаанд зөвхөн ашиглагдах stub-ийг үүсгэдэг тул `lenient()` хэрэглэх шаардлагагүй, strict шалгалт хэвээр ажиллана.

Matcher хэрэглэсэн `send(...)` дуудлагуудын бүх аргумент `eq`, `anyString`, `contains` эсвэл `capture` matcher ашигласан.

Эцсийн `results/mvn-test.txt` гаралт:

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Хэсэг B Мини мутаци

Мутаци бүрийг starter-ийн эх код дээр тусад нь хийж `mvn test` ажиллуулсан. Мутаци бүрийн дараа эх кодыг сэргээсэн; дараагийн мутаци өмнөх өөрчлөлттэй давхардаагүй. Доорх нэрс нь дээрх A тестүүдийн методтой харгалзана.

| Мутант | Түр өөрчилсөн код | Унасан тестүүд | Бодит үр дүн | Нотолгоо |
|---|---|---|---|---|
| 1 | `if (!student.active()) ...` шалгалтыг комментлосон | A3; A8-ийн `INACTIVE_STUDENT` мөр | Tests run: 10, Failures: 2, Errors: 0, Skipped: 0; BUILD FAILURE | [mvn-test-mutant-1.txt](results/mvn-test-mutant-1.txt) |
| 2 | `repository.saveRegistration(studentId, courseId)` дуудалтыг комментлосон | A1; A5 | Tests run: 10, Failures: 2, Errors: 0, Skipped: 0; BUILD FAILURE | [mvn-test-mutant-2.txt](results/mvn-test-mutant-2.txt) |
| 3 | Эхний `if`-ээс `|| courseId == null` хэсгийг хассан | A6, зөвхөн `courseId=null` нөхцөлийн assertion | Tests run: 10, Failures: 1, Errors: 0, Skipped: 0; BUILD FAILURE | [mvn-test-mutant-3.txt](results/mvn-test-mutant-3.txt) |

Мутант 1 дээр идэвхгүй оюутныг цааш нэвтрүүлснээр `ERROR_INACTIVE_STUDENT`-ийн оронд `ERROR_NO_COURSE` буцсан. Мутант 2 дээр `OK` буцаж, и-мэйл явсан ч `saveRegistration` дуудагдаагүйг A1 ба A5 илрүүлсэн. Мутант 3 дээр `courseId=null` үед exception шидэхгүй болсныг A6 илрүүлсэн; A6-г анхнаас нь хоёр аргументын null-ийг тусад нь шалгахаар бичсэн тул мутацийн дараа засвар нэмэх шаардлага гараагүй. Энэ удаагийн гурван мутацийн уналт бүгд assertion/verification failure бөгөөд `UnnecessaryStubbingException` error гараагүй. Сонгосон 3 мутантаас 3-ыг илрүүлсэн нь эдгээр эвдрэлд тест мэдрэг байгааг батлах боловч бүх боломжит алдааг барина гэсэн баталгаа биш.

## Дахин ажиллуулах ба Git шалгалт

Төслийн үндсэн хавтаснаас:

```bash
mvn test
# Гаралтыг дахин хадгалах бол Bash дээр:
set -o pipefail
mvn -B -Dstyle.color=never test 2>&1 | tee results/mvn-test.txt
```

## Дүгнэлт

Mock нь Mockito-гоор үүсгэсэн орлуулагч бөгөөд хариу утга болон дуудлагыг тест бүрд тохируулж, харилцан үйлчлэлийг шалгах боломжтой. Гараар бичсэн fake нь жишээ нь санах ойн repository шиг хялбаршуулсан бодит ажиллагаатай хэрэгжүүлэлт тул олон тестэд дахин ашиглагдаж болох ч түүнийг өөрийг нь зөв байлгах ажил нэмэгдэнэ. Энэ лабораторид repository ба и-мэйл илгээгчийг mock хийснээр өгөгдлийн сан, сүлжээний төлөвөөс хамааралгүйгээр үйлчилгээний шийдвэрийг шалгасан. Харин тестлэгдэж буй `CourseRegistrationService`, `Student` зэрэг утгын объект, `List` зэрэг энгийн өгөгдлийн бүтцийг mock хийхгүй байх нь бодит логик ба өгөгдлийг шалгахад тустай. Зөвхөн `OK` буцсаныг шалгахад хадгалалт хасагдсан эвдрэл нуугдах тул шаардлагатай `verify` шалгалт нэмэх нь утгатай. Гэхдээ бүх дотоод дуудлагын тоо, дарааллыг хэт нарийн баталгаажуулбал зан төлөв өөрчлөгдөөгүй refactoring хүртэл тестийг хэврэг болгож болно. Гурван мутацийн бодит уналт болон сэргээсний дараах ногоон ажиллуулалт нь сонгосон дүрмүүдийг тестүүд үнэхээр шалгаж байгааг нотолсон.

