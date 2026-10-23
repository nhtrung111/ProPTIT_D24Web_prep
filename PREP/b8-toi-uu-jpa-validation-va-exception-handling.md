# Buổi 8: TỐI ƯU JPA, VALIDATION VÀ EXCEPTION HANDLING

---

### 1. Các phương pháp tối ưu JPA

Dưới đây là chi tiết chuyên sâu cho phần đầu tiên theo nội dung từ bài viết của ProPTIT.

## 1. Các phương pháp tối ưu JPA

### 1.1. Tải Tạm Hoãn (Lazy Loading)

Tải Tạm Hoãn có nghĩa là các thực thể liên quan chỉ được tìm nạp khi chúng được truy cập lần đầu tiên. Hibernate sử dụng các đối tượng proxy để đạt được hành vi này.

* **Đặc điểm mặc định**: Đây là cấu hình mặc định cho các mối quan hệ `@OneToMany` và `@ManyToMany`.
* **Cơ chế**: Việc tìm nạp xảy ra theo yêu cầu, giúp giảm bộ nhớ ban đầu và tải cơ sở dữ liệu.

**Ví dụ Code minh họa:**

```java
@Entity
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Employee> employees;
}

```

* **Giải thích đoạn code**: Cấu hình `fetch = FetchType.LAZY` yêu cầu Hibernate **không** tải danh sách nhân viên (`employees`) cho đến khi phương thức `getEmployees()` thực sự được gọi ở đâu đó trong code của bạn.

**Chi tiết Lợi ích:**

* Chỉ có dữ liệu cần thiết mới được tải lên, qua đó cải thiện hiệu suất khởi động của ứng dụng.
* Bộ nhớ (RAM) chỉ bị tiêu thụ khi dữ liệu thực sự được truy cập, làm cho phương pháp này vô cùng tối ưu cho các bộ dữ liệu lớn.
* Vì chỉ tải những gì cần thiết nên băng thông truyền đi cũng được giảm thiểu.

**Chi tiết Hạn chế:**

* Khi dữ liệu cuối cùng được yêu cầu, nó có thể gây ra hiện tượng trễ (latency) vì lúc này dữ liệu bổ sung mới bắt đầu được tải.
* Có nguy cơ dẫn đến nhiều lệnh gọi vào cơ sở dữ liệu liên tục, làm tăng tổng số yêu cầu (đây là nguyên nhân gốc rễ của lỗi N+1 query).
* Yêu cầu logic phức tạp hơn ở tầng Service/Controller để xử lý việc tải dữ liệu bị trì hoãn.

**Các Trường Hợp Sử Dụng (Use Cases):**

* **Bảng tin Mạng xã hội**: Chỉ tải text của bảng tin và trì hoãn việc tìm nạp hình ảnh hoặc video cho đến khi người dùng cuộn tới hoặc nhấp vào.
* **Trang web nặng nội dung**: Phù hợp cho các trang tin tức hoặc e-commerce, nơi các thông tin như đánh giá, chi tiết sản phẩm sâu hoặc hình ảnh bổ sung chỉ được tải khi người dùng thực sự tương tác với chúng.

### 1.2. Tải Nhanh (Eager Loading)

Tải Nhanh là một chiến lược mà tất cả dữ liệu cần thiết (bao gồm cả các dữ liệu liên quan) được tải ngay từ đầu trong một yêu cầu duy nhất. Chiến lược này đảm bảo bạn có thể truy cập tức thì vào tất cả thông tin cần thiết.

* **Đặc điểm mặc định**: Là cấu hình mặc định cho các mối quan hệ `@ManyToOne` và `@OneToOne`.
* **Cơ chế**: Hibernate sẽ thực hiện các truy vấn `JOIN` hoặc các truy vấn bổ sung để tìm nạp tức thời các dữ liệu liên kết.

**Ví dụ Code minh họa:**

```java
@Entity
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;
}

```

* **Giải thích đoạn code**: Cấu hình `fetch = FetchType.EAGER` sẽ đảm bảo rằng thông tin phòng ban (`department`) được tải ngay lập tức cùng với đối tượng Nhân viên (`Employee`) khi bạn truy xuất Nhân viên đó.

**Chi tiết Lợi ích:**

* Tất cả dữ liệu được tải ngay từ đầu, đảm bảo không bao giờ có độ trễ khi cần truy cập dữ liệu liên quan về sau.
* Thường có thể làm giảm tổng số lượng truy vấn cơ sở dữ liệu bằng cách gom chung và tìm nạp tất cả dữ liệu trong một lần duy nhất.
* Dữ liệu luôn có sẵn ngay lập tức nên bạn không cần phải thiết kế logic phức tạp để xử lý việc tải dữ liệu.

**Chi tiết Hạn chế:**

* Do phải tải lượng dữ liệu lớn cùng lúc, thời gian khởi động ứng dụng hoặc tải trang ban đầu sẽ bị chậm hơn đáng kể.
* Việc tìm nạp toàn bộ dữ liệu liên quan đồng loạt có thể làm bộ nhớ RAM bị tiêu thụ nhiều hơn.
* Nếu bạn truy xuất rất nhiều dữ liệu nhưng cuối cùng lại không dùng đến chúng, điều này sẽ dẫn đến sự lãng phí nghiêm trọng trong cả năng lực xử lý dữ liệu và băng thông mạng.

### 1.3. Pagination (Phân trang)

Trong thực tế phát triển phần mềm, một bảng dữ liệu (như bảng `Orders`, `Products`, `Users`) có thể chứa hàng triệu bản ghi. Nếu bạn viết câu lệnh lấy toàn bộ (`SELECT * FROM ...`) rồi đẩy lên RAM ứng dụng, hệ thống sẽ ngay lập tức đối mặt với nguy cơ tràn bộ nhớ (**OutOfMemoryError - OOM**), làm sập ứng dụng và khiến thời gian phản hồi (latency) tăng vọt. **Pagination (Phân trang)** ra đời để giải quyết triệt để bài toán này.

* **Cơ chế hoạt động**: Thay vì lấy tất cả, phân trang giúp chia nhỏ dữ liệu thành các tập hợp nhỏ hơn (gọi là các *page*). Truy vấn gửi xuống Database sẽ chỉ lấy đúng số lượng bản ghi giới hạn cho một trang cụ thể bằng cách sử dụng các từ khóa `LIMIT` và `OFFSET` (hoặc cấu trúc tương đương tùy thuộc vào hệ quản trị cơ sở dữ liệu).
* **Triển khai trong Spring Data JPA**:
  * Spring cung cấp sẵn giao diện **`Pageable`** và lớp **`PageRequest`** cực kỳ mạnh mẽ.
  * Khi truyền đối tượng `Pageable` vào trong Repository method, Spring Data JPA sẽ tự động sinh ra hai câu truy vấn:
    1. Truy vấn lấy dữ liệu của trang hiện tại (áp dụng limit/offset).
    2. Truy vấn đếm tổng số bản ghi (`SELECT COUNT(...)`) để tính tổng số trang (`totalPages`), giúp frontend biết có bao nhiêu trang để hiển thị thanh điều hướng.

**Ví dụ code minh họa:**

```java
// Khai báo trong Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByStatus(String status, Pageable pageable);
}

// Sử dụng tại tầng Service
public Page<User> getUsers(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
    return userRepository.findByStatus("ACTIVE", pageable);
}

```

* **Lợi ích cốt lõi**:
* Giảm tải áp lực cho mạng và bộ nhớ RAM của server.
* Giúp tăng tốc độ phản hồi (response time) của API, đem lại trải nghiệm mượt mà cho người dùng cuối (UI).

### 1.4. Caching (Bộ nhớ đệm)

Truy xuất cơ sở dữ liệu (disk I/O) luôn là một trong những tác vụ tốn kém thời gian nhất của hệ thống backend. **Caching** là giải pháp lưu trữ các dữ liệu thường xuyên được sử dụng (hoặc ít thay đổi) vào một vùng nhớ tạm thời (RAM) siêu nhanh. Khi có request tiếp theo yêu cầu dữ liệu đó, hệ thống sẽ trả về trực tiếp từ cache mà không cần phải chạm vào Database nữa.

Trong hệ sinh thái Hibernate / JPA, Caching được chia thành các cấp độ chính:

#### 1. First-level Cache (Bộ nhớ đệm cấp 1)

* **Phạm vi**: Gắn liền với vòng đời của một **`EntityManager`** hoặc một **`Session`** giao dịch (Transaction).
* **Đặc điểm**:
* Được bật **mặc định** trong JPA/Hibernate và không thể tắt.
* Nếu trong cùng một transaction, bạn gọi hàm `findById()` nhiều lần cho cùng một ID, Hibernate chỉ thực hiện câu truy vấn `SELECT` vào database ở lần đầu tiên; các lần sau, nó lấy thẳng đối tượng đã được lưu trữ trong Persistence Context của Session hiện tại.



#### 2. Second-level Cache (Bộ nhớ đệm cấp 2)

* **Phạm vi**: Toàn cục cho toàn bộ ứng dụng (**Application-wide**), có thể được chia sẻ giữa nhiều Session và Transaction khác nhau.
* **Đặc điểm**:
* Mặc định **đã bị tắt**, bạn cần phải cấu hình và tích hợp thêm các thư viện cung cấp cache bên thứ ba (như **Ehcache**, **Redis**, hoặc **Hazelcast**).
* Giúp giảm thiểu tối đa các câu truy vấn đọc dữ liệu lặp đi lặp lại ở phạm vi toàn hệ thống.


#### 3. Query Cache (Bộ nhớ đệm truy vấn)

* **Đặc điểm**: Phối hợp cùng Second-level Cache để lưu trữ kết quả của một câu truy vấn cụ thể (ví dụ: kết quả trả về của một danh sách ID thỏa mãn điều kiện tìm kiếm). Nếu cùng một câu truy vấn với các tham số tương tự được gọi lại, Hibernate sẽ lấy tập hợp ID đó từ cache thay vì thực thi lại câu lệnh SQL.

* *First-level Cache*: Bộ nhớ đệm mặc định của Hibernate, có phạm vi tồn tại trong một `Session` hoặc `Transaction`.
* *Second-level Cache*: Bộ nhớ đệm chia sẻ trên toàn ứng dụng, cần phải cài đặt bên thứ 3 (như Ehcache hoặc Redis).
* *Query Cache*: Chỉ định lưu trữ kết quả của các câu truy vấn cụ thể.

### 1.5. Manual / Native Query (Truy vấn thủ công / Truy vấn thuần)

Mặc dù Spring Data JPA và JPQL (Java Persistence Query Language) rất mạnh mẽ trong việc tự động hóa các thao tác hướng đối tượng, nhưng trong nhiều tình huống thực tế, chúng lại bộc lộ hạn chế:

* Câu lệnh SQL sinh tự động không được tối ưu cho hệ quản trị cơ sở dữ liệu (DBMS) cụ thể bạn đang dùng (MySQL, PostgreSQL, Oracle...).
* Bạn cần sử dụng các hàm toán học phức tạp, câu lệnh tổng hợp (Aggregate functions), CTE (Common Table Expressions) hoặc tối ưu hóa thông qua các **Index** đặc thù của riêng Database đó.

Lúc này, giải pháp tối ưu là sử dụng **Native Query** (viết câu lệnh SQL thuần thực thụ) thông qua annotation **`@Query`** với thuộc tính `nativeQuery = true`.

**Ví dụ code minh họa:**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Sử dụng Native SQL trực tiếp
    @Query(value = "SELECT * FROM users WHERE status = :status AND age > :minAge", nativeQuery = true)
    List<User> findActiveUsersWithNativeQuery(@Param("status") String status, @Param("minAge") int minAge);
    
}

```

**Ưu điểm và Lưu ý:**

* **Ưu điểm**: Khai thác tối đa hiệu năng của cơ sở dữ liệu, giải quyết triệt để các bài toán truy vấn hiệu năng cao, phức tạp mà JPQL không hỗ trợ.
* **Hạn chế**: Làm mất tính độc lập của ORM (nếu sau này bạn đổi từ MySQL sang PostgreSQL, câu lệnh SQL thuần có thể bị lỗi cú pháp). Do đó, chỉ nên dùng Native Query khi thực sự cần thiết cho việc tối ưu hiệu năng.

### 1.6. Dynamic Query (Truy vấn động)

Trong các màn hình tìm kiếm hoặc lọc dữ liệu (Filter/Search) thực tế, người dùng có thể nhập rất nhiều tiêu chí khác nhau nhưng **không bắt buộc** (ví dụ: lúc chỉ nhập tên, lúc nhập cả ngày sinh và trạng thái, lúc lại để trống tất cả).

Nếu sử dụng các phương pháp Repository tĩnh thông thường, bạn sẽ phải viết hàng chục hàm tổ hợp khác nhau cho mọi trường hợp, code sẽ trở nên rối rắm và không thể bảo trì. **Dynamic Query** giải quyết vấn đề này bằng cách cho phép bạn linh hoạt xây dựng câu lệnh truy vấn tại thời điểm chạy (runtime) dựa trên các tham số thực tế mà người dùng truyền lên.

Spring Data JPA hỗ trợ truy vấn động thông qua hai cơ chế phổ biến nhất:

#### Cơ chế 1: Sử dụng `Specification` và `Criteria API` (Mặc định trong Spring Data JPA)

* **Cách hoạt động**: Kết hợp `JpaSpecificationExecutor` vào Repository và tự định nghĩa các điều kiện (`Predicate`) bằng Java code một cách linh hoạt.

**Ví dụ minh họa:**

```java
// 1. Khai báo interface Repository kế thừa JpaSpecificationExecutor
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
}

// 2. Viết logic động tại tầng Service
public List<User> searchUsers(String name, String status) {
    return userRepository.findAll((root, query, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        // Chỉ thêm điều kiện lọc tên nếu người dùng có truyền vào
        if (name != null && !name.isEmpty()) {
            predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
        }
        
        // Chỉ thêm điều kiện lọc trạng thái nếu có
        if (status != null && !status.isEmpty()) {
            predicates.add(criteriaBuilder.equal(root.get("status"), status));
        }
        
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    });
}

```

#### Cơ chế 2: Sử dụng thư viện ngoài **QueryDSL**

* **Ưu điểm**: Viết truy vấn động bằng cú pháp dạng hướng đối tượng an toàn về kiểu dữ liệu (Type-safe), trực quan và dễ đọc hơn rất nhiều so với `Criteria API` nguyên thủy.

Dưới đây là phần chi tiết chuyên sâu về **Phòng tránh N + 1 query** và **Override default fetch plan với `JOIN FETCH` / `Entity Graph**` trong các phương pháp tối ưu JPA:

### 1.7. Phòng tránh N + 1 Query Problem

**Vấn đề N + 1 query là gì?**
Đây là một trong những lỗi về hiệu năng (performance bottleneck) phổ biến nhất khi làm việc với ORM (Hibernate/JPA).

* Giả sử bạn có 1 phòng ban (`Department`) và danh sách các nhân viên (`Employee`) trong phòng ban đó theo mối quan hệ `@OneToMany` (mặc định là `LAZY`).
* Khi bạn truy vấn lấy danh sách **100 phòng ban**, Hibernate sẽ thực hiện **1 câu truy vấn** đầu tiên để lấy toàn bộ danh sách phòng ban này (`Query 1`).
* Sau đó, nếu bạn duyệt qua danh sách 100 phòng ban này và gọi đến thuộc tính `getEmployees()` để xem nhân viên của từng phòng, Hibernate sẽ phải phát sinh thêm **100 câu truy vấn** riêng lẻ nữa để lấy nhân viên cho từng phòng (`N` queries).
* Tổng số câu lệnh SQL chạy xuống Database sẽ là **1 + 100 = 101 truy vấn**, làm sụt giảm nghiêm trọng hiệu năng hệ thống.

**Các phương pháp phòng tránh và khắc phục:**

1. **Sử dụng `JOIN FETCH` trong JPQL**: Ép Hibernate gom tất cả dữ liệu liên quan vào chung **một câu truy vấn duy nhất** bằng lệnh `JOIN` ở cấp độ SQL.
2. **Sử dụng `@EntityGraph**`: Giải pháp khai báo bằng Annotation giúp giải quyết triệt để N+1 mà không cần phải viết câu lệnh JPQL phức tạp.
3. **Sử dụng `@BatchSize**`:
* Nếu bạn có một thực thể và không muốn dùng `JOIN FETCH`, bạn có thể cấu hình `@BatchSize(size = 20)` trên tập hợp liên quan.
* Thay vì chạy 100 truy vấn riêng lẻ cho 100 phòng ban, Hibernate sẽ gom nhóm chúng lại và chỉ chạy **5 câu truy vấn** dạng `SELECT * FROM employees WHERE department_id IN (?, ?, ..., ?)` (mỗi lần lấy 20 bản ghi).

### 1.8. Override Default Fetch Plan với `JOIN FETCH` / `Entity Graph`

Trong thực tế, bạn thường để cấu hình fetch mặc định của các mối quan hệ là **`LAZY`** để tiết kiệm tài nguyên khi khởi tạo. Tuy nhiên, tại một số màn hình/chức năng đặc thù, bạn **bắt buộc phải lấy luôn dữ liệu liên quan** ngay từ đầu để tránh lỗi `LazyInitializationException` hoặc tránh lỗi N+1 Query.

Thay vì đổi cấu hình `EAGER` toàn cục (điều này rất nguy hiểm và gây lãng phí bộ nhớ ở các chỗ khác), bạn có thể **ghi đè (override) kế hoạch tải dữ liệu (fetch plan)** ngay tại thời điểm gọi câu truy vấn. Hai cách phổ biến nhất là:

#### Cách 1: Dùng `JOIN FETCH` trong JPQL

Bạn tự viết câu truy vấn tùy chỉnh tại Repository và sử dụng từ khóa `JOIN FETCH` để chỉ định tường minh cho Hibernate biết cần lấy luôn dữ liệu của bảng liên kết.

**Ví dụ code minh họa:**

```java
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    // Ghi đè cấu hình LAZY bằng JOIN FETCH để lấy luôn danh sách nhân viên trong 1 câu truy vấn duy nhất
    @Query("SELECT d FROM Department d JOIN FETCH d.employees WHERE d.id = :id")
    Department findByIdWithEmployees(@Param("id") Long id);
    
}

```

* **Cơ chế**: Hibernate sẽ thực hiện câu lệnh `SQL INNER JOIN` hoặc `LEFT JOIN` giữa bảng `Department` và bảng `Employee`, trả về toàn bộ dữ liệu chỉ với **1 lần gọi Database duy nhất**.

#### Cách 2: Dùng `Entity Graph`

Nếu bạn không muốn viết câu lệnh JPQL phức tạp hoặc câu truy vấn quá dài, **`@EntityGraph`** là giải pháp thay thế cực kỳ thanh lịch và hiện đại do Spring Data JPA cung cấp.

* Bạn chỉ cần khai báo annotation `@EntityGraph` trên phương thức của Repository và chỉ định tên các thuộc tính cần tải (fetch) ngay lúc đó.

**Ví dụ code minh họa:**

```java
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    // Sử dụng Entity Graph để ép Hibernate tải kèm trường "employees" và "projects"
    @EntityGraph(attributePaths = {"employees", "projects"})
    Optional<Department> findById(Long id);
    
}

```

* **Ưu điểm của Entity Graph**:
* Giúp giữ cho file Repository sạch sẽ, dễ đọc.
* Tách biệt rõ ràng giữa cấu hình Entity tĩnh và nhu cầu lấy dữ liệu động theo từng API.
* Có thể tái sử dụng thông qua annotation `@NamedEntityGraph` được định nghĩa trực tiếp trên lớp Entity.

### 2. Tìm hiểu về Validation

* **Thư viện**: Spring Boot cung cấp hỗ trợ qua `spring-boot-starter-validation`, tự động thêm phiên bản tương thích của **Hibernate Validator** (triển khai tiêu chuẩn của Bean Validation).
* **Hoạt động cốt lõi**: Xác định tính hợp lệ của dữ liệu bằng cách đặt các ràng buộc (constraints) trực tiếp lên các trường (field) của một class.
* **Các Annotation phổ biến**:
* `@NotNull`: Trường không được `null`.
* `@NotEmpty`: Collection hoặc Array không được rỗng.
* `@NotBlank`: Chuỗi string không được rỗng và phải có ít nhất 1 ký tự không phải khoảng trắng.
* `@Min` / `@Max`: Kiểm tra số lớn hơn/nhỏ hơn một giá trị.
* `@Pattern`: Yêu cầu chuỗi khớp với biểu thức chính quy (Regex).
* `@Email`: Định dạng phải là email hợp lệ.


* **Áp dụng**: Có thể dùng `Validator` thủ công (ném ra `ConstraintViolationException` nếu vi phạm) hoặc dùng `@Validated` (cấp độ class) và `@Valid` (trên các tham số) để Spring tự động kiểm tra `Request Body`, `Path Variables`, và `Request Parameters`.

---

### 3. Tìm hiểu về Exception handling

Exception Handling trong Spring giúp xử lý tập trung tất cả các ngoại lệ bị ném ra trong quá trình chạy ứng dụng (ví dụ: khi Validation thất bại, hoặc khi tìm không thấy dữ liệu trong Database).

* Sử dụng annotation `@RestControllerAdvice` (hoặc `@ControllerAdvice`) kết hợp cùng `@ExceptionHandler` để bắt các Exception cụ thể.
* Cách này tránh việc phải lặp lại các khối lệnh `try-catch` trong mọi Controller. Nó giúp trả về các HTTP Status chuẩn xác (như 400 Bad Request, 500 Internal Server Error) đi kèm một định dạng phản hồi (JSON) đồng nhất cho phía client.

---

### 4. Override default fetch plan với join fetch / entity graph

Nếu một Entity đang được cấu hình mặc định là `Lazy Loading`, nhưng ở một tính năng nhất định bạn muốn lấy luôn dữ liệu liên quan để tránh N+1 Query, bạn có thể ghi đè (override) kế hoạch tải mặc định này:

* **Join Fetch**: Trong câu lệnh JPQL (ví dụ: `@Query("SELECT d FROM Department d JOIN FETCH d.employees")`), từ khóa `JOIN FETCH` ép Hibernate không dùng proxy mà thực hiện kết nối bảng (`JOIN`) và lấy luôn toàn bộ dữ liệu chỉ trong một câu truy vấn duy nhất.
* **Entity Graph**: Một giải pháp tương đương `JOIN FETCH` nhưng khai báo trực tiếp và gọn gàng hơn. Bằng cách gắn `@EntityGraph(attributePaths = {"employees"})` lên trên tên hàm trong Repository, Spring tự động hiểu rằng phải tải Eager (tải nhanh) trường "employees" ở đúng câu truy vấn này.

*(Ghi chú: Bài viết tổng hợp bao gồm trích xuất trực tiếp từ các bài blog của ProPTIT kết hợp với các lý thuyết nền tảng chuẩn của Spring Boot/JPA nhằm đảm bảo không bị khuyết thiếu bất kì thông tin nào trong danh sách bạn đã yêu cầu).*

Hai phần này **mới chỉ được tóm tắt ngắn gọn** ở câu trả lời tổng hợp ban đầu và **chưa được đi sâu vào chi tiết code cũng như cơ chế hoạt động**.

Dưới đây là phần trình bày chi tiết chuyên sâu về **Validation** và **Exception Handling trong Spring** theo đúng nội dung từ nguồn bài viết:

## 5. Tìm hiểu về Validation

Trong một ứng dụng Web/REST API, việc kiểm tra dữ liệu đầu vào (Input Validation) từ phía client gửi lên là bước bắt buộc để đảm bảo tính toàn vẹn của dữ liệu trước khi đưa xuống tầng Business hoặc Database.

### 5.1. Sử Dụng Spring Boot Validation Starter

Spring Boot cung cấp hỗ trợ **Bean Validation** thông qua `validation starter`.

* **Khai báo trong Gradle**:
```groovy
implementation('org.springframework.boot:spring-boot-starter-validation')

```


* **Lưu ý**: Nếu dự án của bạn đã bao gồm `spring-boot-starter-web`, dependency này sẽ được tự động thêm vào. Bản chất `validation starter` sử dụng **Hibernate Validator**, đây là triển khai phổ biến và chuẩn mực nhất của đặc tả Bean Validation.

### 5.2. Các Bean Validation Annotations Thường Dùng

Bean Validation hoạt động bằng cách đặt các ràng buộc (constraints) trực tiếp lên các trường (`field`) của class thông qua Annotation:

| Annotation | Mô tả chi tiết |
| --- | --- |
| **`@NotNull`** | Trường dữ liệu không được phép là `null`. |
| **`@NotEmpty`** | Áp dụng cho Collection hoặc Array, yêu cầu không được rỗng (phải có ít nhất 1 phần tử). |
| **`@NotBlank`** | Áp dụng cho kiểu `String`, yêu cầu không được rỗng và phải có ít nhất một ký tự không phải khoảng trắng. |
| **`@Min` / `@Max**` | Giới hạn giá trị số phải lớn hơn hoặc nhỏ hơn một hạn mức nhất định. |
| **`@Pattern`** | Yêu cầu chuỗi `String` phải khớp với một biểu thức chính quy (`regex`). |
| **`@Email`** | Xác thực định dạng chuỗi `String` phải là một địa chỉ email hợp lệ. |

**Ví dụ khai báo trên Entity/DTO:**

```java
public class CustomerDto {
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Tên không được để trống")
    private String name;
}

```

### 5.3. Validator (Kiểm tra thủ công)

Ngoài cách để Spring tự động validate qua các annotation, bạn có thể gọi trực tiếp đối tượng `Validator` trong code khi cần kiểm tra thủ công:

```java
Set<ConstraintViolation<CustomerDto>> violations = validator.validate(customer);
if (!violations.isEmpty()) {
    throw new ConstraintViolationException(violations);
}

```

### 5.4. Phân biệt `@Validated` và `@Valid`

* **`@Valid`**: Đặt trên tham số phương thức hoặc trường (field) để yêu cầu Spring thực hiện validate đối tượng đó (thường dùng cho Request Body).
* **`@Validated`**: Là annotation cấp class (đặt trên Controller hoặc Service), dùng để kích hoạt tính năng validate cho các tham số dạng đơn lẻ (như Path Variable, Request Param) truyền vào phương thức của class đó.

### 5.5. Validation Input cho Spring MVC Controller

Trong một HTTP Request gửi đến Controller, có 3 loại input chính cần validate:

1. **Request Body** (POST/PUT): Dùng `@Valid` ngay trước `@RequestBody`.
2. **Path Variables & Request Parameters** (GET/DELETE/URL parameters): Đặt `@Validated` ở cấp độ class Controller, sau đó gắn các annotation như `@Min`, `@Pattern` trực tiếp lên tham số của method trong Controller.

---

## 6. Tìm hiểu về Exception Handling

Khi xảy ra lỗi trong ứng dụng (ví dụ: dữ liệu client gửi lên không hợp lệ, không tìm thấy bản ghi trong CSDL, hoặc lỗi hệ thống bất ngờ), việc để nguyên stack trace trả về client là cực kỳ nguy hiểm về mặt bảo mật và kém chuyên nghiệp. **Exception Handling** giúp bắt và xử lý tập trung tất cả các ngoại lệ.

### 6.1. Vấn đề khi không xử lý tập trung

Nếu không có cơ chế tập trung, bạn sẽ phải viết hàng loạt các khối lệnh `try-catch` lặp đi lặp lại ở mọi phương thức trong Controller, dẫn đến code rối rắm, khó bảo trì và định dạng lỗi trả về cho client mỗi nơi một kiểu.

### 6.2. Giải pháp tập trung với `@RestControllerAdvice` và `@ExceptionHandler`

Spring cung cấp cơ chế toàn cục (Global Exception Handling) thông qua việc kết hợp 2 annotation:

* **`@RestControllerAdvice`**: Đánh dấu một lớp chuyên trách đóng vai trò như một "trạm trung chuyển" lắng nghe toàn bộ các Exception được ném ra từ tất cả các Controller trong hệ thống.
* **`@ExceptionHandler`**: Đặt trên phương thức để chỉ định hàm đó sẽ xử lý riêng cho một loại Exception cụ thể nào đó.

**Ví dụ code minh họa chuẩn:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt lỗi khi dữ liệu gửi lên vi phạm các annotation Validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST); // Trả về HTTP Status 400
    }

    // Bắt lỗi tùy chỉnh khi không tìm thấy tài nguyên (Ví dụ: Không tìm thấy User)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND); // Trả về HTTP Status 404
    }
}

```

### 6.3. Lợi ích cốt lõi của Exception Handling

* **Đồng nhất định dạng (JSON Response)**: Giúp phía Frontend (React, Angular, Mobile app) luôn nhận được cấu trúc thông báo lỗi chuẩn xác (gồm mã lỗi, thông điệp, HTTP status).
* **Tách biệt mối quan tâm (Separation of Concerns)**: Tầng Business/Controller chỉ tập trung xử lý logic nghiệp vụ sạch sẽ, việc đóng gói lỗi được đẩy toàn bộ sang Global Exception Handler.