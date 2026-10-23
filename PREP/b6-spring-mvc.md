# Buổi 6: Spring Boot cơ bản

---

## 1. Spring Bean & IoC Container

### 1.1. Spring Bean là gì?

Spring Bean là các objects được quản lý, khởi tạo, cấu hình và lắp ráp bởi Spring IoC Container. 
Thay vì tạo đối tượng thủ công bằng từ khóa new thì Spring sẽ tự quản lý object này.

### 1.2. BeanFactory & ApplicationContext

#### 1.2.1 BeanFactory là gì?

- BeanFactory: Container cơ bản nhất trong Spring, cung cấp cơ chế Configuration Framework và quản lý các Bean theo cơ chế Lazy Initialization (chỉ khởi tạo Bean khi được gọi getBean()).

#### 1.2.2 ApplicationContext là gì?

- ApplicationContext: Được xây dựng dựa trên BeanFactory, bổ sung nhiều tính năng cho ứng dụng thực tế. Trong Spring Boot, ApplicationContext được dùng làm Container chính và khởi tạo các Bean theo kiểu Eager Initialization (khởi tạo ngay khi ứng dụng startup, ngoại trừ những Bean được đánh dấu @Lazy).

### 1.3. Vòng đời của một Spring Bean (Bean Lifecycle)

Từ khi ứng dụng khởi chạy đến khi dừng lại, một Bean trải qua 5 giai đoạn chính:

1. Instantiation (Khởi tạo): Spring Container gọi constructor để tạo instance của Bean.

2. Populate Properties (Bơm phụ thuộc): Spring inject các dependencies vào Bean thông qua Setter, Field, hoặc Constructor Injection (@Autowired).

3. Aware Interfaces (Nhận diện ngữ cảnh): Nếu Bean implement các interface *Aware (như BeanNameAware, ApplicationContextAware), Spring sẽ truyền các thông tin ngữ cảnh tương ứng vào Bean.

4. Initialization (Khởi tạo nâng cao):
   - Các phương thức đánh dấu @PostConstruct được thực thi.
   - Phương thức afterPropertiesSet() của interface InitializingBean chạy (nếu có).
   - Phương thức custom init (initMethod) định nghĩa trên @Bean chạy.
   
5. Destruction (Hủy bỏ): Khi ApplicationContext đóng lại (shutdown):
   - Phương thức đánh dấu @PreDestroy thực thi. 
   - Phương thức destroy() của interface DisposableBean chạy.

- Văn bản & Mã nguồn: HTML, CSS, JavaScript, JSON, XML.

- Hình ảnh & Đa phương tiện: PNG, JPG, MP4, Audio.

- Tệp tin: PDF, ZIP, các file dữ liệu binary.

Khi mở một trang web, trình duyệt không chỉ gửi 1 request. Nó gửi request đầu tiên để lấy file HTML về, sau đó đọc file HTML thấy có ảnh, CSS, JS... nó tiếp tục gửi các request con nữa để lấy đủ các file đó về ghép lại thành trang web hoàn chỉnh.

## 2. Spring MVC: @Controller & Thymeleaf

### 2.1. @Controller:

@Controller là annotation dùng để đánh dấu một Class là Controller trong mô hình MVC. \
Nó chịu trách nhiệm tiếp nhận HTTP Request, xử lý logic (hoặc gọi Service), sau đó trả về tên của một View (UI template) cùng với dữ liệu đi kèm (Model).

### 2.2. Thymeleaf:

Thymeleaf là một công cụ tạo mẫu (template engine) mã nguồn mở bằng Java, dùng để xử lý và hiển thị giao diện HTML, XML cho các ứng dụng web.

Đặc điểm nổi bật: 
- Hiển thị trực tiếp: File HTML chứa Thymeleaf có thể mở trực tiếp bằng trình duyệt mà không cần chạy server.  
- Tích hợp tốt với Spring Boot: Thường dùng thay thế cho JSP trong các dự án Spring Boot.
- Cú pháp tự nhiên: Sử dụng các thuộc tính trong thẻ HTML có sẵn (th:*), không làm hỏng cấu trúc HTML gốc.
Ưu điểm chính:
- Giúp thiết kế giao diện dễ dàng hơn nhờ sự hỗ trợ từ các designer (vì file vẫn chuẩn HTML).
- Hỗ trợ cơ chế cache giúp tối ưu tốc độ tải trang.

## 3. Một số Annotation trong SpingBoot: @SpringBootApplication, @Component, @Bean, @Autowired

|**Annotation**|**Mục đích & Chi tiết**|
|----------|-------------------|
|**@SpringBootApplication**|Annotation tổ hợp ở class chạy main. Bao gồm 3 annotation quan trọng:• @Configuration: Đánh dấu lớp cấu hình Spring.• @EnableAutoConfiguration: Bật tính năng tự động cấu hình dựa trên thư viện trong classpath.• @ComponentScan: Quét các Component/Bean trong package hiện tại và các package con.|
|**@Component**|"Annotation mức Class, đánh dấu một Java class là một Spring Bean để Spring Container tự động phát hiện (component scan) và quản lý. (Các annotation @Service, @Repository, @Controller đều là stereotype kế thừa từ @Component)."|
|**@Bean**|"Annotation mức Method, dùng trong các class @Configuration. Dùng để khai báo và khởi tạo thủ công một Bean (thường dùng khi cấu hình các thư viện bên thứ 3 hoặc custom cấu hình phức tạp)."|
|**@Autowired**|"Dùng để tự động Inject (tiêm) một Bean phụ thuộc vào một Bean khác. Có thể đặt ở Field, Setter, hoặc Constructor (Constructor Injection là best practice)."|

## 4. Lombok Library

Lombok là một thư viện Java hoạt động dưới dạng **Annotation Processor** (bộ xử lý chú thích tại thời điểm biên dịch - *Compile-time*).  
Khi build dự án, Lombok can thiệp vào quá trình dịch mã nguồn (`.java` -> `.class`), đọc các annotation và tự động generate mã bytecode tương ứng vào file `.class`. Nhờ đó, file `.java` tuy ngắn gọn, nhưng khi chạy, ứng dụng vẫn có đầy đủ phương thức getter, setter, constructor,...

### 1. `@Getter` & `@Setter`

Tự động tạo các phương thức truy cập (`get...()`) và thiết lập (`set...()`) cho các thuộc tính.

* **Phạm vi đặt:**
  * **Cấp Class:** Sinh getter/setter cho *tất cả* các field trong class.
  * **Cấp Field:** Chỉ sinh getter/setter cho đúng field đó.

* **Cấu hình AccessLevel:** có thể giới hạn phạm vi truy cập bằng tham số `value`:
```java
@Getter
@Setter
public class User {
    private String username;

    // field này chỉ có getter public, nhưng setter là protected
    @Setter(AccessLevel.PROTECTED)
    private String password;
}

```

* **Lưu ý kiểu boolean:** Nếu field kiểu `boolean` (ví dụ `boolean active`), getter sinh ra sẽ có dạng `isActive()` thay vì `getActive()`. Nếu kiểu là wrapper `Boolean`, getter vẫn là `getActive()`.

### 2. `@ToString`

Tự động `@Override` phương thức `toString()` để trả về chuỗi đại diện cho đối tượng dưới dạng `ClassName(fieldName1=val1, fieldName2=val2)`.

* **Các tham số cấu hình quan trọng:**
  * `exclude`: Loại bỏ các field không muốn in ra (rất quan trọng với các thông tin nhạy cảm như `password` hoặc các mối quan hệ lặp vô tận trong Hibernate/JPA).
  * `callSuper = true`: In thêm các thuộc tính của class cha.

* **Ví dụ:**
```java
@ToString(exclude = "password")
public class User {
    private Long id;
    private String username;
    private String password; // Sẽ KHÔNG xuất hiện trong kết quả toString()
}

```

### 3. Các Constructor Annotation (`@*Constructor`)

Lombok giúp tạo constructor tự động mà không cần viết tay:

#### a. `@NoArgsConstructor`

* Tạo **Constructor không tham số** (ví dụ `public User() {}`).

#### b. `@AllArgsConstructor`

* Tạo **Constructor nhận tất cả các fields** theo đúng thứ tự khai báo trong class.

#### c. `@RequiredArgsConstructor`

* Tạo **Constructor chỉ nhận các tham số bắt buộc**. Tham số bắt buộc là:
1. Các field được đánh dấu `final` (chưa được gán giá trị ban đầu).
2. Các field có gắn `@NonNull`.


* **Ứng dụng cực cao trong Spring Boot:** Dùng để thực hiện **Constructor Injection** thay cho `@Autowired` trên field.

```java
@Service
@RequiredArgsConstructor // Lombok tự tạo constructor nhận UserService & UserRepository
public class OrderService {
    // 2 dependency này là final, sẽ tự động được inject qua Constructor
    private final UserRepository userRepository;
    private final PaymentService paymentService;
}

```

### 4. `@Builder`

Tự động áp dụng **Builder Pattern** cho class. Giúp khởi tạo đối tượng một cách linh hoạt, rõ ràng, tránh việc gọi constructor dài khiến dễ nhầm thứ tự tham số.

* **Code Lombok:**
```java
@Getter
@Builder
public class Product {
    private Long id;
    private String name;
    private Double price;

    @Builder.Default 
    private String status = "ACTIVE"; // Gán giá trị mặc định khi dùng Builder
}

```

* **Cách sử dụng thực tế:**
```java
Product p = Product.builder()
                   .name("Laptop Dell")
                   .price(1500.0)
                   .build(); // Không cần truyền id hay status

```

### 5. `@Data`

`@Data` là một **Shortcut Annotation** (Annotation tổ hợp). Viết `@Data` tương đương với việc bạn khai báo đồng thời:

1. `@Getter` (trên tất cả field)
2. `@Setter` (trên tất cả field non-final)
3. `@ToString`
4. `@EqualsAndHashCode` (sinh ra 2 phương thức `equals()` và `hashCode()`)
5. `@RequiredArgsConstructor`

```java
// Dùng cho DTO, Request/Response payload
@Data
public class UserDTO {
    private String username;
    private String email;
}

```

#### CẢNH BÁO: Rủi ro khi sử dụng `@Data` với JPA Entity

Dù `@Data` rất tiện lợi cho các lớp DTO, nhưng không nên dùng `@Data` trên các `@Entity` của Spring Data JPA vì:

1. **Lặp vô tận (StackOverflowError):** `@ToString` và `@EqualsAndHashCode` do `@Data` tạo ra sẽ truy cập vào tất cả các thuộc tính. Nếu Entity có quan hệ 2 chiều (ví dụ `@OneToMany` - `@ManyToOne`), nó sẽ gọi vòng lặp vô tận giữa 2 entity dẫn đến tràn bộ nhớ.
2. **Hiệu năng JPA:** `@EqualsAndHashCode` kiểm tra tất cả thuộc tính có thể kích hoạt Lazy Loading ngoài ý muốn, làm giảm hiệu năng query Database.
3. **Giải pháp tốt nhất cho Entity:** Dùng kết hợp `@Getter`, `@Setter`, `@NoArgsConstructor`, và override `@ToString` (có exclude quan hệ) thay vì dùng `@Data`.

## 5. Logging trong Spring Boot: Log4j & @Slf4j
Log4j là gì?
Log4j (và phiên bản nâng cấp Log4j2) là thư viện logging dựa trên Java phổ biến, cho phép ghi lại các sự kiện/nhật ký hoạt động của ứng dụng ra Console, File, hoặc Database theo các cấp độ log khác nhau.

Các level logging tiêu chuẩn (từ thấp đến cao):
TRACE < DEBUG < INFO < WARN < ERROR < FATAL

Lưu ý trong Spring Boot: Mặc định spring-boot-starter-logging sử dụng SLF4J làm Logging Abstraction (Facade) và Logback làm Logging Implementation. Nếu muốn dùng Log4j2, bạn cần exclude Logback starter và thêm starter log4j2.

Annotation @Slf4j
@Slf4j là annotation do Lombok cung cấp. Khi đặt trên một Class, Lombok tự động sinh ra một logger instance có tên log tại thời điểm biên dịch bằng facade org.slf4j.Logger.