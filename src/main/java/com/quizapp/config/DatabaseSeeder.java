package com.quizapp.config;

import com.quizapp.entity.*;
import com.quizapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SubjectRepository subjectRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Demo User
        if (userRepository.count() == 0) {
            User demoUser = User.builder()
                    .username("admin")
                    .email("admin@quizapp.com")
                    .password(passwordEncoder.encode("password"))
                    .role("ROLE_USER")
                    .unlockedLevel(1)
                    .build();
            userRepository.save(demoUser);
            System.out.println("🌱 Database Seeder: Created default user 'admin' with password 'password'");
        }

        // 2. Seed Units & Questions
        if (unitRepository.count() == 0) {
            System.out.println("🌱 Database Seeder: Seeding Units and MCQ Question Bank...");
        Subject javaSubject = Subject.builder()
        .name("Java")
        .build();
javaSubject = subjectRepository.save(javaSubject);

// Seed Unit 1
Unit u1 = Unit.builder()
        .name("Unit 1: Fundamentals of Programming")
        .subject(javaSubject)
        .levelNumber(1)
        .description("Basic syntax, variables, data types, and operators.")
        .build();
u1 = unitRepository.save(u1);
seedUnit1Questions(u1);

// Seed Unit 2
Unit u2 = Unit.builder()
        .name("Unit 2: Object-Oriented Design")
        .subject(javaSubject)
        .levelNumber(2)
        .description("Classes, inheritance, polymorphism, and encapsulation.")
        .build();
u2 = unitRepository.save(u2);
seedUnit2Questions(u2);

// Seed Unit 3
Unit u3 = Unit.builder()
        .name("Unit 3: Data Structures & Collections")
        .subject(javaSubject)
        .levelNumber(3)
        .description("Lists, Maps, Sets, and operations on Java Collections.")
        .build();
u3 = unitRepository.save(u3);
seedUnit3Questions(u3);

           

            // Seed Unit 4
            Unit u4 = Unit.builder()
                    .name("Unit 4: Advanced Database & JPA")
                    .subject(javaSubject)
                    .levelNumber(4)
                    .description("Relational databases, JPA mappings, and SQL operations.")
                    .build();
            u4 = unitRepository.save(u4);
            seedUnit4Questions(u4);
            
            System.out.println("🌱 Database Seeder: Seeded all 4 levels and question templates successfully!");
        }
    }

    private void seedUnit1Questions(Unit unit) {
        List<Question> list = new ArrayList<>();
        
        // EASY
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("EASY")
                .questionText("Which data type is used to create a variable that should store text in Java?")
                .optionA("char").optionB("String").optionC("txt").optionD("boolean")
                .correctAnswer("B").build());
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("EASY")
                .questionText("Which operator is used for checking equality of values in Java?")
                .optionA("=").optionB("==").optionC("equals").optionD("is")
                .correctAnswer("B").build());

        // MEDIUM
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("MEDIUM")
                .questionText("What is the default value of a local variable inside a method in Java?")
                .optionA("0").optionB("null").optionC("No default value (must be initialized)").optionD("Depends on JVM")
                .correctAnswer("C").build());
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("MEDIUM")
                .questionText("Which loop is guaranteed to execute at least once in Java?")
                .optionA("for loop").optionB("while loop").optionC("do-while loop").optionD("foreach loop")
                .correctAnswer("C").build());

        // HARD
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("HARD")
                .questionText("What is the outcome of evaluates statement (5 / 2) in Java programming?")
                .optionA("2.5").optionB("2").optionC("3").optionD("Compiler Error")
                .correctAnswer("B").build());
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("HARD")
                .questionText("Which of these statements is true about the Java garbage collection?")
                .optionA("It guarantees that the memory will never run out").optionB("Developers can force instant GC anytime using System.gc()").optionC("It automatically reclaims heap memory of unreferenced objects").optionD("It deletes files and releases system handles automatically")
                .correctAnswer("C").build());

        questionRepository.saveAll(list);
    }

    private void seedUnit2Questions(Unit unit) {
        List<Question> list = new ArrayList<>();
        
        // EASY
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("EASY")
                .questionText("Which keyword is used to inherit a class in Java?")
                .optionA("implements").optionB("extends").optionC("inherits").optionD("super")
                .correctAnswer("B").build());
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("EASY")
                .questionText("What is encapsulation in Object-Oriented Programming?")
                .optionA("Sharing class variables").optionB("Combining data and methods operating on that data inside a single unit").optionC("Allowing functions to take multiple forms").optionD("Executing processes in parallel threads")
                .correctAnswer("B").build());

        // MEDIUM
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("MEDIUM")
                .questionText("Which polymorphism type is Method Overloading associated with in Java?")
                .optionA("Runtime Polymorphism").optionB("Compile-time Polymorphism").optionC("Dynamic Abstraction").optionD("Standard Inheritance")
                .correctAnswer("B").build());
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("MEDIUM")
                .questionText("What is the difference between an Interface and an Abstract Class in Java 8?")
                .optionA("Abstract classes cannot have instance variables").optionB("Interfaces can have default and static method implementations").optionC("Interfaces allow private constructors").optionD("Abstract classes do not support constructors")
                .correctAnswer("B").build());

        // HARD
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("HARD")
                .questionText("Which keyword prevents a class from being inherited or overridden in Java?")
                .optionA("static").optionB("abstract").optionC("final").optionD("private")
                .correctAnswer("C").build());
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("HARD")
                .questionText("What occurs when a subclass method overrides a superclass method, but the reference is of type superclass?")
                .optionA("Subclass method is called at runtime").optionB("Superclass method is called at runtime").optionC("Compiler raises an error").optionD("JVM throws ClassCastException")
                .correctAnswer("A").build());

        questionRepository.saveAll(list);
    }

    private void seedUnit3Questions(Unit unit) {
        List<Question> list = new ArrayList<>();
        
        // EASY
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("EASY")
                .questionText("Which Java Collection allows unique elements only?")
                .optionA("List").optionB("Set").optionC("Map").optionD("Queue")
                .correctAnswer("B").build());

        // MEDIUM
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("MEDIUM")
                .questionText("Which of these classes implements the List interface using a doubly-linked list?")
                .optionA("ArrayList").optionB("LinkedList").optionC("Vector").optionD("Stack")
                .correctAnswer("B").build());
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("MEDIUM")
                .questionText("What data structure does a HashMap use internally for key lookups?")
                .optionA("Binary Tree").optionB("Doubly Linked List").optionC("Hash Table").optionD("Graph")
                .correctAnswer("C").build());

        // HARD
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("HARD")
                .questionText("What is the average time complexity for searching a key in a TreeMap?")
                .optionA("O(1)").optionB("O(log N)").optionC("O(N)").optionD("O(N log N)")
                .correctAnswer("B").build());

        questionRepository.saveAll(list);
    }

    private void seedUnit4Questions(Unit unit) {
        List<Question> list = new ArrayList<>();
        
        // EASY
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("EASY")
                .questionText("Which JPA annotation is used to specify the primary key of an entity?")
                .optionA("@Column").optionB("@Id").optionC("@GeneratedValue").optionD("@PrimaryKey")
                .correctAnswer("B").build());

        // MEDIUM
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("MEDIUM")
                .questionText("What does the '@ManyToMany' annotation require to map the relationship correctly in relational databases?")
                .optionA("A Foreign Key column on the source table").optionB("A Join Table specifying foreign keys from both entities").optionC("One-to-One mapping definitions").optionD("A composite primary key on both entities")
                .correctAnswer("B").build());

        // HARD
        list.add(Question.builder().unit(unit).isInbuilt(true).difficulty("HARD")
                .questionText("What is the difference between JPA 'FetchType.LAZY' and 'FetchType.EAGER'?")
                .optionA("LAZY fetches associated records instantly, EAGER delays loading").optionB("LAZY delays loading associated entities until accessed, EAGER loads them immediately").optionC("LAZY only works with MySQL databases").optionD("EAGER prevents caching queries entirely")
                .correctAnswer("B").build());

        questionRepository.saveAll(list);
    }
}
