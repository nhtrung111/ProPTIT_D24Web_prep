# Buổi 9: Thao tác với database

### Mô hình dữ liệu

| Quan hệ | Kiểu | Owning side (giữ khóa ngoại) | Cột / bảng nối |
| --- | --- | --- | --- |
| User – UserProfile | 1-1 | UserProfile | `user_profiles.user_id` |
| Category – Product | 1-N | Product | `products.category_id` |
| User – Order | 1-N | Order | `orders.user_id` |
| Product – Tag | N-N | Product | bảng `product_tags(product_id, tag_id)` |

Ghi nhớ: trong quan hệ 1-N, bên N luôn là owning side vì khóa ngoại nằm trong bảng của nó.

### Dependency (pom.xml)

```xml
<dependencies>
    <!-- Spring Data JPA: kéo theo Hibernate làm JPA provider -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <!-- Driver PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <!-- Lombok sinh getter/setter/constructor lúc biên dịch -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### Cấu hình (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shop
    username: shop
    password: shop
  jpa:
    hibernate:
      ddl-auto: validate        # Môi trường thật: dùng Flyway/Liquibase, không để Hibernate tự tạo bảng
    open-in-view: false         # Tắt OSIV để lỗi lazy loading lộ ra sớm ở tầng service
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 50   # Gom lazy loading thành câu IN (...), giảm N+1
        jdbc:
          batch_size: 30               # Gom INSERT/UPDATE thành batch
        order_inserts: true
        order_updates: true
logging:
  level:
    org.hibernate.SQL: debug                            # In câu SQL thật
    org.hibernate.orm.jdbc.bind: trace                  # In giá trị tham số
```

Bật log SQL ngay từ đầu khi học: đây là cách duy nhất để nhìn thấy N+1 query và kiểm chứng các tối ưu trong tài liệu.

## Phần 1: Mối quan hệ giữa các Entity (JPA Relationships)

Mỗi quan hệ có đúng một owning side: bên giữ cột khóa ngoại và là bên duy nhất Hibernate đọc khi sinh câu INSERT/UPDATE. Bên còn lại khai báo `mappedBy` và chỉ phục vụ điều hướng trong Java.

### 1.1 Quy ước chung khi viết Entity

- Không dùng `@Data` cho Entity: `equals`, `hashCode`, `toString` tự sinh sẽ duyệt qua quan hệ lazy, gây `LazyInitializationException` hoặc `StackOverflowError` do vòng lặp hai chiều.
- Loại các field quan hệ khỏi `toString` bằng `@ToString.Exclude`.
- `equals` so sánh theo `id`; `hashCode` trả về hằng số theo class để không đổi khi `id` được gán sau `persist`.
- Mọi `@ManyToOne` và `@OneToOne` khai báo `fetch = FetchType.LAZY` (mặc định của JPA là EAGER).
- Quan hệ hai chiều luôn có helper method (`addProduct`, `addTag`) cập nhật cả hai phía trong bộ nhớ.
- `@ManyToMany` dùng `Set`, không dùng `List`: với `List`, Hibernate xóa toàn bộ dòng của bảng nối rồi chèn lại mỗi khi collection thay đổi.

### 1.2 Category và Product: @OneToMany / @ManyToOne

```java
package com.example.shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "categories")
@Getter
@Setter
@ToString
@NoArgsConstructor   // JPA bắt buộc có constructor không tham số
@AllArgsConstructor  // Cần cho @Builder
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Dùng cột BIGSERIAL/IDENTITY của DB
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // INVERSE SIDE: "category" là tên FIELD trong Product (không phải tên cột)
    // cascade ALL + orphanRemoval: Category quản lý trọn vòng đời các Product của nó
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default     // Giữ giá trị khởi tạo khi dùng builder, tránh products = null
    private List<Product> products = new ArrayList<>();

    // Helper đồng bộ hai chiều: luôn gán owning side (product.category)
    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setCategory(null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Category category)) {
            return false;
        }
        // Entity mới (id == null) chỉ bằng chính nó
        return id != null && id.equals(category.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

```java
package com.example.shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2) // Tiền tệ luôn dùng BigDecimal
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // OWNING SIDE của Category-Product: bảng products chứa cột category_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Mặc định là EAGER, đổi sang LAZY
    @JoinColumn(
            name = "category_id",                                  // Tên cột FK trong bảng products
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_category")) // Tên constraint khi sinh DDL
    @ToString.Exclude
    private Category category;

    // OWNING SIDE của Product-Tag: Product khai báo @JoinTable
    // Chỉ PERSIST và MERGE: xóa Product KHÔNG được xóa Tag đang dùng chung
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "product_tags",                                 // Bảng trung gian
            joinColumns = @JoinColumn(name = "product_id"),        // FK trỏ về Entity hiện tại
            inverseJoinColumns = @JoinColumn(name = "tag_id"))     // FK trỏ về Entity phía bên kia
    @ToString.Exclude
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    void onCreate() {
        // Callback chạy ngay trước INSERT
        this.createdAt = LocalDateTime.now();
    }

    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getProducts().add(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getProducts().remove(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product product)) {
            return false;
        }
        return id != null && id.equals(product.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

### 1.3 Tag: @ManyToMany

```java
package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "tags")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // INVERSE SIDE: "tags" là tên field trong Product. Không có @JoinTable ở đây.
    // Không cascade: Tag không sở hữu Product
    @ManyToMany(mappedBy = "tags")
    @ToString.Exclude
    @Builder.Default
    private Set<Product> products = new HashSet<>();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tag tag)) {
            return false;
        }
        return id != null && id.equals(tag.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

### 1.4 User, UserProfile (@OneToOne) và Order (@OneToMany)

```java
package com.example.shop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users") // "user" là từ khóa dành riêng trong PostgreSQL
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // INVERSE SIDE của 1-1: khóa ngoại user_id nằm ở bảng user_profiles
    // Lưu ý: Hibernate KHÔNG thể lazy-load @OneToOne phía mappedBy nếu không bật bytecode enhancement,
    // vì nó phải query bảng kia mới biết giá trị là null hay proxy.
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private UserProfile profile;

    // INVERSE SIDE của User-Order
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Gán profile và đồng bộ owning side trong một lệnh
    public void assignProfile(UserProfile newProfile) {
        if (newProfile == null) {
            if (this.profile != null) {
                this.profile.setUser(null);
            }
        } else {
            newProfile.setUser(this);
        }
        this.profile = newProfile;
    }

    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

```java
package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 300)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // OWNING SIDE của 1-1: bảng user_profiles có cột user_id
    // unique = true là thứ biến quan hệ N-1 ở mức DB thành 1-1 thật sự
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private User user;
}
```

```java
package com.example.shop.entity;

public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
```

```java
package com.example.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "orders") // "order" là từ khóa SQL (ORDER BY)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    // STRING thay vì ORDINAL: thêm/đổi thứ tự enum không làm hỏng dữ liệu cũ
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // OWNING SIDE của User-Order: bảng orders chứa cột user_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Order order)) {
            return false;
        }
        return id != null && id.equals(order.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

### 1.5 mappedBy: owning side và inverse side

Owning side là bên Hibernate dùng để quyết định giá trị khóa ngoại; inverse side (có `mappedBy`) bị bỏ qua khi ghi xuống DB.

- `@ManyToOne` luôn là owning side và không có thuộc tính `mappedBy`.
- `@OneToMany` hai chiều phải có `mappedBy`. Nếu thiếu, Hibernate coi đó là một quan hệ một chiều riêng và tạo thêm bảng nối `categories_products`, dẫn đến hai nguồn sự thật cho cùng một quan hệ.
- `@OneToOne` thiếu `mappedBy` ở cả hai bên sẽ sinh hai khóa ngoại (`users.profile_id` và `user_profiles.user_id`) có thể lệch nhau.
- `@ManyToMany`: bên có `@JoinTable` là owning side; chỉ thay đổi `product.getTags()` mới ghi vào `product_tags`.

Hệ quả thực tế: `category.getProducts().add(product)` mà không gọi `product.setCategory(category)` thì cột `category_id` không được ghi. Đó là lý do helper `addProduct` luôn gán cả hai phía.

### 1.6 CascadeType

Cascade lan truyền thao tác của EntityManager từ Entity cha sang Entity con. Nó không liên quan đến `ON DELETE CASCADE` của DB.

| CascadeType | Thao tác được lan truyền | Dùng khi |
| --- | --- | --- |
| `PERSIST` | `persist()`: lưu cha thì con mới (transient) cũng được INSERT | Cha tạo con cùng lúc (User tạo UserProfile) |
| `MERGE` | `merge()`: gộp trạng thái của con detached vào Persistence Context | Nhận object đồ thị từ API rồi lưu lại |
| `REMOVE` | `remove()`: xóa cha thì xóa luôn con | Con không tồn tại độc lập (Order của User bị xóa) |
| `REFRESH` | `refresh()`: đọc lại con từ DB, ghi đè thay đổi chưa flush | Cần đồng bộ lại sau trigger hoặc native update |
| `DETACH` | `detach()`: tách con khỏi Persistence Context | Giải phóng bộ nhớ khi xử lý batch lớn |
| `ALL` | Tất cả các loại trên | Quan hệ cha-con sở hữu hoàn toàn (Category-Product, User-Order) |

- `orphanRemoval = true` mạnh hơn `REMOVE`: chỉ cần gỡ con khỏi collection là con bị DELETE, không cần xóa cha.
- Không đặt `REMOVE` hoặc `ALL` trên `@ManyToOne` và `@ManyToMany`: xóa một Product sẽ kéo theo xóa Category hoặc Tag đang được Product khác dùng.

```java
package com.example.shop.service;

import com.example.shop.entity.Category;
import com.example.shop.entity.Product;
import com.example.shop.entity.User;
import com.example.shop.entity.UserProfile;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CascadeDemoService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Category createCategoryWithProducts() {
        Category category = Category.builder()
                .name("Laptop")
                .description("Máy tính xách tay")
                .build();
        Product thinkPad = Product.builder()
                .name("ThinkPad X1 Carbon")
                .price(new BigDecimal("35000000"))
                .stockQuantity(10)
                .build();
        Product macBook = Product.builder()
                .name("MacBook Air M3")
                .price(new BigDecimal("28000000"))
                .stockQuantity(25)
                .build();
        category.addProduct(thinkPad);
        category.addProduct(macBook);
        // Chỉ save Category: PERSIST (nằm trong ALL) lan truyền INSERT sang 2 Product
        return categoryRepository.save(category);
    }

    @Transactional
    public void removeFirstProduct(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục " + categoryId));
        if (!category.getProducts().isEmpty()) {
            Product firstProduct = category.getProducts().get(0);
            // orphanRemoval: Product bị gỡ khỏi collection sẽ bị DELETE khi transaction commit
            category.removeProduct(firstProduct);
        }
    }

    @Transactional
    public User registerUser(String email, String fullName, String phoneNumber) {
        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .build();
        UserProfile profile = UserProfile.builder()
                .phoneNumber(phoneNumber)
                .build();
        user.assignProfile(profile);
        // Một lệnh save tạo cả bản ghi users và user_profiles
        return userRepository.save(user);
    }
}
```

### 1.7 FetchType: LAZY, EAGER, LazyInitializationException và N+1

`LAZY` trả về proxy và chỉ query khi code chạm vào dữ liệu; `EAGER` tải ngay cùng Entity cha. Mặc định của JPA:

| Annotation | Mặc định | Khuyến nghị |
| --- | --- | --- |
| `@ManyToOne` | EAGER | Đổi sang LAZY |
| `@OneToOne` | EAGER | Đổi sang LAZY |
| `@OneToMany` | LAZY | Giữ LAZY |
| `@ManyToMany` | LAZY | Giữ LAZY |

EAGER không giải quyết được N+1: với JPQL `SELECT p FROM Product p`, Hibernate vẫn chạy 1 câu cho danh sách rồi thêm 1 câu cho mỗi Category để thỏa EAGER. Quyết định tải gì phải nằm ở từng câu truy vấn (JOIN FETCH, `@EntityGraph`), không nằm ở mapping.

**LazyInitializationException** xảy ra khi truy cập proxy chưa khởi tạo sau khi Persistence Context đã đóng.

```java
package com.example.shop.service;

import com.example.shop.entity.Product;
import com.example.shop.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FetchingDemoService {

    private final ProductRepository productRepository;

    // SAI: không có @Transactional, Persistence Context đóng ngay sau findById
    public String getCategoryNameWrong(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm " + productId));
        // product.getCategory() là proxy chưa khởi tạo -> getName() ném LazyInitializationException
        return product.getCategory().getName();
    }

    // ĐÚNG (cách 1): transaction readOnly bao trọn thao tác truy cập lazy
    @Transactional(readOnly = true)
    public String getCategoryNameRight(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm " + productId));
        return product.getCategory().getName(); // Hibernate chạy thêm 1 SELECT categories
    }

    // N+1: 1 câu lấy N sản phẩm + tối đa N câu lấy category
    @Transactional(readOnly = true)
    public List<String> listProductsWithCategoryNPlusOne() {
        List<Product> products = productRepository.findAll(); // Câu 1
        List<String> lines = new ArrayList<>();
        for (Product product : products) {
            // Mỗi category chưa nằm trong Persistence Context sinh thêm 1 SELECT
            lines.add(product.getName() + " - " + product.getCategory().getName());
        }
        return lines;
    }

    // ĐÚNG (cách 2): một câu JOIN FETCH duy nhất, xem Phần 3
    @Transactional(readOnly = true)
    public List<String> listProductsWithCategoryOptimized() {
        List<Product> products = productRepository.findAllWithCategory();
        List<String> lines = new ArrayList<>();
        for (Product product : products) {
            lines.add(product.getName() + " - " + product.getCategory().getName()); // Không query thêm
        }
        return lines;
    }
}
```

Các cách xử lý N+1, theo thứ tự ưu tiên:

1. `JOIN FETCH` trong JPQL cho use case cụ thể (Phần 3).
2. `@EntityGraph(attributePaths = {"category"})` trên method của Repository (Phần 2).
3. Trả về DTO projection, chỉ lấy đúng cột cần dùng (Phần 3, Phần 4).
4. `hibernate.default_batch_fetch_size`: lưới an toàn, gom N câu thành khoảng N/50 câu `WHERE id IN (...)`.

Không dùng `spring.jpa.open-in-view=true` hay `hibernate.enable_lazy_load_no_trans` để che lỗi: chúng giữ kết nối DB suốt request hoặc mở transaction mới cho mỗi lần lazy load.

### 1.8 @JoinColumn và @JoinTable

`@JoinColumn` đặt trên owning side để cấu hình cột khóa ngoại:

- `name`: tên cột FK (mặc định `<tên field>_<tên cột id>`, ví dụ `category_id`).
- `referencedColumnName`: cột được tham chiếu bên bảng kia, mặc định là khóa chính.
- `nullable`, `unique`: ràng buộc khi sinh DDL; `unique = true` biến FK thành quan hệ 1-1.
- `foreignKey = @ForeignKey(name = "...")`: đặt tên constraint thay cho tên ngẫu nhiên.
- `insertable = false, updatable = false`: cột chỉ đọc, dùng khi cùng một cột được map hai lần.

`@JoinTable` chỉ dùng cho `@ManyToMany` (hoặc `@OneToMany` một chiều):

- `name`: tên bảng trung gian (mặc định `products_tags`).
- `joinColumns`: FK trỏ về Entity đang khai báo (owning side), ở đây `product_id`.
- `inverseJoinColumns`: FK trỏ về Entity bên kia, ở đây `tag_id`.

Khi bảng trung gian cần thêm cột (ngày gắn thẻ, người gắn), hãy bỏ `@ManyToMany` và tạo Entity riêng `ProductTag` với hai `@ManyToOne`.

## Phần 2: Derived Query Methods

Spring Data JPA đọc tên method lúc khởi động ứng dụng, tách nó thành các thuộc tính và từ khóa, rồi sinh JPQL tương ứng. Tên sai thuộc tính sẽ làm ứng dụng không khởi động được, nên lỗi lộ ra sớm thay vì lúc chạy.

### 2.1 Cấu trúc tên method

Một tên method gồm ba phần: `<chủ ngữ>By<điều kiện><sắp xếp>`.

- Chủ ngữ: `find`, `read`, `get`, `query`, `stream` (trả Entity), `count`, `exists`, `delete`. Có thể chèn `Distinct`, `TopN`, `FirstN`: `findTop5By`, `findDistinctBy`.
- Điều kiện: chuỗi `<Thuộc tính><Toán tử>` nối bằng `And` / `Or`. Thiếu toán tử nghĩa là `=`.
- Sắp xếp: `OrderBy<Thuộc tính>Asc|Desc`, có thể nối nhiều: `OrderByPriceDescNameAsc`.
- Tham số `Pageable` và `Sort` ở cuối danh sách tham số được áp dụng tự động, không xuất hiện trong tên.

| Từ khóa | Ví dụ | JPQL sinh ra |
| --- | --- | --- |
| `GreaterThan` / `LessThanEqual` | `findByPriceGreaterThan` | `p.price > ?1` |
| `Between` | `findByPriceBetween` | `p.price BETWEEN ?1 AND ?2` |
| `Containing` + `IgnoreCase` | `findByNameContainingIgnoreCase` | `UPPER(p.name) LIKE UPPER('%' + ?1 + '%')` |
| `StartingWith` / `EndingWith` | `findByNameStartingWith` | `p.name LIKE ?1 + '%'` |
| `In` / `NotIn` | `findByIdIn` | `p.id IN ?1` |
| `IsNull` / `IsNotNull` | `findByDescriptionIsNull` | `c.description IS NULL` |
| `Not` | `findByStatusNot` | `o.status <> ?1` |
| `True` / `False` | `findByActiveTrue` | `p.active = true` |
| `Before` / `After` | `findByCreatedAtAfter` | `o.createdAt > ?1` |

### 2.2 Truy vấn lồng qua thuộc tính liên kết

Với `findByCategoryName`, Spring Data thử tìm thuộc tính `categoryName` trong Product. Không có, nó tách camelCase từ phải sang: `category` + `name`, rồi sinh `JOIN p.category c WHERE c.name = ?1`.

Khi tên mơ hồ (ví dụ Product có cả field `categoryName` lẫn `category.name`), dùng dấu gạch dưới để chỉ định ranh giới: `findByCategory_Name`. Duyệt qua collection cũng được: `findByTagsName` sinh `JOIN p.tags t WHERE t.name = ?1`.

### 2.3 Các Repository cơ bản

```java
package com.example.shop.repository;

import com.example.shop.entity.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);
}
```

```java
package com.example.shop.repository;

import com.example.shop.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

### 2.4 ProductRepository

`ProductRepository` kế thừa thêm `ProductCustomRepository`, một fragment dùng `EntityManager` được định nghĩa ở Phần 5. Nếu chỉ chạy Phần 2, hãy bỏ interface này khỏi mệnh đề `extends`.

```java
package com.example.shop.repository;

import com.example.shop.entity.Product;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, ProductCustomRepository {

    // (1) So sánh đơn giản: WHERE p.price > ?1
    List<Product> findByPriceGreaterThan(BigDecimal price);

    // (2) Tìm theo từ khóa không phân biệt hoa thường: UPPER(p.name) LIKE UPPER('%?1%')
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // (3) Kết hợp Between và And: giá trong khoảng VÀ còn hàng
    List<Product> findByPriceBetweenAndStockQuantityGreaterThan(
            BigDecimal minPrice, BigDecimal maxPrice, Integer minStock);

    // (4) Kết hợp Or: tên bắt đầu bằng tiền tố HOẶC giá thấp hơn ngưỡng
    List<Product> findByNameStartingWithOrPriceLessThan(String prefix, BigDecimal price);

    // (5) Toán tử In với một Collection
    List<Product> findByIdIn(Collection<Long> ids);

    // (6) Top + OrderBy: 5 sản phẩm đắt nhất (Hibernate thêm FETCH FIRST 5 ROWS ONLY)
    List<Product> findTop5ByOrderByPriceDesc();

    // (7) Truy vấn lồng: JOIN p.category c WHERE c.name = ?1
    List<Product> findByCategoryName(String categoryName);

    // (8) First + truy vấn lồng + OrderBy: sản phẩm mới nhất của một danh mục
    Optional<Product> findFirstByCategoryNameOrderByCreatedAtDesc(String categoryName);

    // (9) Gạch dưới chỉ rõ ranh giới thuộc tính lồng; nhiều điều kiện + sắp xếp tăng dần
    List<Product> findByCategory_IdAndPriceLessThanEqualOrderByPriceAsc(Long categoryId, BigDecimal maxPrice);

    // (10) Duyệt qua collection N-N; Distinct loại bản ghi trùng khi một sản phẩm có nhiều tag khớp
    List<Product> findDistinctByTagsNameIn(Collection<String> tagNames);

    // (11) Phân trang: Spring tự sinh thêm câu COUNT để tính tổng số trang
    Page<Product> findByCategoryNameAndStockQuantityGreaterThan(
            String categoryName, Integer minStock, Pageable pageable);

    // (12) Đếm và kiểm tra tồn tại: không tải Entity vào bộ nhớ
    long countByCategoryName(String categoryName);

    boolean existsByNameIgnoreCase(String name);

    // (13) @EntityGraph: vẫn là derived query nhưng fetch category trong cùng câu SELECT (LEFT JOIN)
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByStockQuantityGreaterThan(Integer minStock);

    // JOIN FETCH viết tay, được dùng ở ví dụ N+1 của Phần 1; giải thích chi tiết ở Phần 3
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    List<Product> findAllWithCategory();
}
```

### 2.5 OrderRepository

```java
package com.example.shop.repository;

import com.example.shop.entity.Order;
import com.example.shop.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Tìm theo khóa nghiệp vụ duy nhất
    Optional<Order> findByOrderCode(String orderCode);

    // Lọc theo FK: Hibernate 6 dùng thẳng cột orders.user_id, không JOIN sang users
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // In với enum
    List<Order> findByStatusIn(Collection<OrderStatus> statuses);

    // Between với thời gian (hai đầu đều bao gồm)
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Truy vấn lồng sang User: JOIN o.user u WHERE u.email = ?1 AND o.status = ?2
    List<Order> findByUserEmailAndStatus(String email, OrderStatus status);

    // Top + nhiều điều kiện + sắp xếp giảm dần
    List<Order> findTop3ByUserIdAndStatusOrderByTotalAmountDesc(Long userId, OrderStatus status);

    // Not + GreaterThanEqual
    List<Order> findByTotalAmountGreaterThanEqualAndStatusNot(BigDecimal minAmount, OrderStatus excludedStatus);

    // Phân trang theo trạng thái
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, OrderStatus status);
}
```

### 2.6 Sử dụng trong Service

```java
package com.example.shop.service;

import com.example.shop.entity.Order;
import com.example.shop.entity.OrderStatus;
import com.example.shop.entity.Product;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Mặc định chỉ đọc: Hibernate bỏ qua dirty checking, tiết kiệm bộ nhớ
public class CatalogQueryService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public Page<Product> browseCategory(String categoryName, int pageNumber, int pageSize) {
        // Trang bắt đầu từ 0; sắp xếp truyền qua Pageable thay vì viết vào tên method
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "price"));
        return productRepository.findByCategoryNameAndStockQuantityGreaterThan(categoryName, 0, pageable);
    }

    public List<Product> searchByKeyword(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword.trim());
    }

    public List<Order> openOrders() {
        return orderRepository.findByStatusIn(List.of(OrderStatus.PENDING, OrderStatus.PAID));
    }
}
```

### 2.7 Ưu điểm và giới hạn

Ưu điểm:

- Không viết câu truy vấn; tên method chính là tài liệu.
- Kiểm tra lúc khởi động: đổi tên field mà quên đổi method thì ứng dụng báo lỗi ngay.
- Tự động hỗ trợ `Pageable`, `Sort`, `Optional`, `Stream`, `Slice`.

Giới hạn:

- Tên dài khó đọc khi quá 2-3 điều kiện. Ví dụ (9) đã là giới hạn hợp lý.
- Không có `JOIN FETCH`, `GROUP BY`, hàm tổng hợp ngoài `count`, subquery hay DTO constructor.
- Không nhóm được điều kiện bằng ngoặc: `A And B Or C` luôn hiểu là `(A AND B) OR C`.
- `deleteBy...` tải từng Entity vào bộ nhớ rồi xóa từng dòng; xóa hàng loạt nên dùng `@Modifying` (Phần 4).

Quy tắc thực hành: tên method vượt quá khoảng 3 điều kiện, cần JOIN FETCH hoặc cần DTO thì chuyển sang `@Query` (Phần 3). Bộ lọc tùy chọn (người dùng có thể bỏ trống) thì dùng Criteria API hoặc Specification (Phần 5).

## Phần 3: JPQL với @Query

JPQL viết truy vấn trên Entity và field Java; Hibernate dịch sang SQL của CSDL đang dùng. `@Query` là lựa chọn mặc định khi derived query không đủ: cần JOIN FETCH, GROUP BY, DTO hoặc điều kiện phức tạp.

### 3.1 JPQL khác SQL thuần thế nào

| Tiêu chí | JPQL | SQL thuần (Native) |
| --- | --- | --- |
| Đối tượng thao tác | Entity và field: `Product p`, `p.category.name` | Bảng và cột: `products p`, `p.category_id` |
| JOIN | Theo quan hệ đã map: `JOIN p.category c`, không cần `ON` | Phải viết `ON p.category_id = c.id` |
| Phân biệt hoa thường | Tên Entity và field phân biệt hoa thường | Tùy CSDL |
| Tính di động | Chạy trên mọi CSDL Hibernate hỗ trợ | Gắn với một CSDL cụ thể |
| Kiểm tra cú pháp | Lúc khởi động ứng dụng | Chỉ khi câu lệnh chạy |
| Kết quả | Entity được quản lý, DTO, scalar | Entity (nếu đủ cột), `Object[]`, interface projection |
| Tính năng đặc thù DB | Hạn chế (Hibernate 6 đã có CTE, window function trong HQL) | Đầy đủ |

Lưu ý: `ORDER` là từ khóa dành riêng trong đặc tả JPA. Hibernate 6 vẫn chấp nhận `FROM Order o`; nếu cần di động sang provider khác, đặt `@Entity(name = "CustomerOrder")`.

### 3.2 DTO dùng cho Constructor Expression

Java record là lựa chọn gọn nhất cho DTO chỉ đọc. Thứ tự và kiểu tham số của constructor phải khớp chính xác với biểu thức `SELECT new`.

```java
package com.example.shop.dto;

import java.math.BigDecimal;

public record ProductSummaryDto(Long id, String name, BigDecimal price, String categoryName) {
}
```

```java
package com.example.shop.dto;

import java.math.BigDecimal;

// COUNT trả Long, AVG trả Double, MAX giữ nguyên kiểu cột (BigDecimal)
public record CategoryStatisticsDto(String categoryName, Long productCount, Double averagePrice, BigDecimal maxPrice) {
}
```

```java
package com.example.shop.dto;

import java.math.BigDecimal;

public record UserOrderSummaryDto(Long userId, String email, Long orderCount, BigDecimal totalSpent) {

    // Compact constructor: SUM trên tập rỗng trả về null, chuẩn hóa thành 0
    public UserOrderSummaryDto {
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }
    }
}
```

### 3.3 ProductJpqlRepository

Các ví dụ JPQL được đặt trong interface riêng để mỗi phần đọc độc lập. Spring Data cho phép nhiều Repository cùng quản lý một Entity; trong dự án thật, gộp chúng vào `ProductRepository`.

```java
package com.example.shop.repository;

import com.example.shop.dto.CategoryStatisticsDto;
import com.example.shop.dto.ProductSummaryDto;
import com.example.shop.entity.Category;
import com.example.shop.entity.Product;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductJpqlRepository extends JpaRepository<Product, Long> {

    // NAMED PARAMETERS: :tên khớp với @Param("tên"); dễ đọc, không phụ thuộc thứ tự
    @Query("""
            SELECT p FROM Product p
            WHERE p.price BETWEEN :minPrice AND :maxPrice
              AND p.stockQuantity > 0
            ORDER BY p.price ASC
            """)
    List<Product> findAvailableInPriceRange(@Param("minPrice") BigDecimal minPrice,
                                            @Param("maxPrice") BigDecimal maxPrice);

    // INDEXED PARAMETERS: ?1, ?2 theo thứ tự tham số của method; dễ nhầm khi thêm tham số mới
    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', ?1, '%'))
              AND p.category.id = ?2
            """)
    List<Product> searchInCategory(String keyword, Long categoryId);

    // JOIN thường: chỉ dùng để LỌC, category KHÔNG được nạp vào Product (vẫn là proxy lazy)
    @Query("SELECT p FROM Product p JOIN p.category c WHERE c.name = :categoryName")
    List<Product> findByCategoryNameJoin(@Param("categoryName") String categoryName);

    // JOIN FETCH quan hệ N-1: nạp Product và Category trong 1 câu SQL, giải quyết N+1
    @Query("SELECT p FROM Product p JOIN FETCH p.category c WHERE c.id = :categoryId")
    List<Product> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId);

    // JOIN FETCH collection N-N: LEFT để giữ cả sản phẩm chưa có tag
    // Hibernate 6 tự loại bản ghi cha trùng lặp; DISTINCT giữ lại để tương thích Hibernate 5
    @Query("""
            SELECT DISTINCT p FROM Product p
            JOIN FETCH p.category
            LEFT JOIN FETCH p.tags
            WHERE p.id IN :ids
            """)
    List<Product> findAllWithDetailsByIdIn(@Param("ids") Collection<Long> ids);

    // Kiểu trả về không bắt buộc trùng Entity của Repository: nạp Category kèm toàn bộ Product
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.products WHERE c.id = :categoryId")
    Optional<Category> findCategoryWithProducts(@Param("categoryId") Long categoryId);

    // DTO CONSTRUCTOR EXPRESSION: chỉ SELECT 4 cột, kết quả không được Persistence Context quản lý
    @Query("""
            SELECT new com.example.shop.dto.ProductSummaryDto(p.id, p.name, p.price, c.name)
            FROM Product p JOIN p.category c
            WHERE p.price >= :minPrice
            ORDER BY p.price DESC
            """)
    List<ProductSummaryDto> findSummariesFromPrice(@Param("minPrice") BigDecimal minPrice);

    // DTO + GROUP BY + HAVING: thống kê theo danh mục
    @Query("""
            SELECT new com.example.shop.dto.CategoryStatisticsDto(c.name, COUNT(p), AVG(p.price), MAX(p.price))
            FROM Category c LEFT JOIN c.products p
            GROUP BY c.id, c.name
            HAVING COUNT(p) >= :minProducts
            ORDER BY COUNT(p) DESC
            """)
    List<CategoryStatisticsDto> statisticsByCategory(@Param("minProducts") long minProducts);

    // PHÂN TRANG + DTO: phải tự viết countQuery vì Spring không suy ra được COUNT từ SELECT new
    @Query(
            value = """
                    SELECT new com.example.shop.dto.ProductSummaryDto(p.id, p.name, p.price, c.name)
                    FROM Product p JOIN p.category c
                    WHERE c.id = :categoryId
                    """,
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Page<ProductSummaryDto> findSummaryPageByCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    // BƯỚC 1 của phân trang an toàn với collection: chỉ phân trang trên id
    @Query(
            value = "SELECT p.id FROM Product p WHERE p.category.id = :categoryId",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    Page<Long> findIdPageByCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    // SORT động: Spring nối ORDER BY từ tham số Sort vào cuối câu JPQL
    @Query("SELECT p FROM Product p WHERE p.stockQuantity >= :minStock")
    List<Product> findInStock(@Param("minStock") Integer minStock, Sort sort);
}
```

### 3.4 OrderJpqlRepository

```java
package com.example.shop.repository;

import com.example.shop.dto.UserOrderSummaryDto;
import com.example.shop.entity.Order;
import com.example.shop.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderJpqlRepository extends JpaRepository<Order, Long> {

    // JOIN FETCH user: hiển thị danh sách đơn kèm email người mua mà không N+1
    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.user u
            WHERE o.status = :status
              AND o.createdAt >= :fromDate
            ORDER BY o.createdAt DESC
            """)
    List<Order> findRecentWithUser(@Param("status") OrderStatus status,
                                   @Param("fromDate") LocalDateTime fromDate);

    // LEFT JOIN ... ON: điều kiện đặt trong ON để vẫn giữ người dùng chưa có đơn hợp lệ
    // (đặt trong WHERE sẽ biến LEFT JOIN thành INNER JOIN)
    @Query("""
            SELECT new com.example.shop.dto.UserOrderSummaryDto(u.id, u.email, COUNT(o), SUM(o.totalAmount))
            FROM User u
            LEFT JOIN u.orders o ON o.status IN :statuses
            GROUP BY u.id, u.email
            ORDER BY SUM(o.totalAmount) DESC NULLS LAST
            """)
    List<UserOrderSummaryDto> summarizeSpending(@Param("statuses") Collection<OrderStatus> statuses);
}
```

### 3.5 Phân trang khi có JOIN FETCH collection

Ghép `JOIN FETCH` collection (`p.tags`, `c.products`) với `Pageable` là lỗi phổ biến. SQL `LIMIT` cắt theo dòng kết quả, mà một Product có 3 tag chiếm 3 dòng. Hibernate vì thế tải toàn bộ dữ liệu vào RAM rồi mới cắt trang, kèm cảnh báo `HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory`.

JOIN FETCH quan hệ N-1 (`p.category`) với `Pageable` thì an toàn vì mỗi Product vẫn chỉ một dòng. Với collection, dùng kỹ thuật hai bước:

```java
package com.example.shop.service;

import com.example.shop.entity.Product;
import com.example.shop.repository.ProductJpqlRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPagingService {

    private final ProductJpqlRepository productJpqlRepository;

    public Page<Product> pageWithTags(Long categoryId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("price").descending());

        // Bước 1: LIMIT/OFFSET chạy trên id ở mức SQL, kèm 1 câu COUNT
        Page<Long> idPage = productJpqlRepository.findIdPageByCategory(categoryId, pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Bước 2: nạp đầy đủ category + tags cho đúng các id của trang này
        List<Product> products = productJpqlRepository.findAllWithDetailsByIdIn(idPage.getContent());

        // Câu IN (...) không giữ thứ tự: sắp xếp lại theo thứ tự id của bước 1
        Map<Long, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Long> orderedIds = idPage.getContent();
        List<Product> orderedProducts = orderedIds.stream()
                .map(productById::get) // Tra Map O(1) cho mỗi id
                .toList();

        return new PageImpl<>(orderedProducts, pageable, idPage.getTotalElements());
    }
}
```

Map `productById` giúp sắp xếp lại theo thứ tự của bước 1 với độ phức tạp O(n), thay vì gọi `indexOf` trong vòng lặp (O(n²)).

### 3.6 Lưu ý khi viết JPQL

- Luôn dùng tham số (`:name`, `?1`), không nối chuỗi: nối chuỗi mở đường cho JPQL injection và phá cache câu lệnh.
- Không JOIN FETCH hai collection kiểu `List` cùng lúc: Hibernate ném `MultipleBagFetchException`. Đổi một bên sang `Set` hoặc tách thành hai câu.
- Không gán alias cho JOIN FETCH rồi lọc trên alias đó trong WHERE (`JOIN FETCH p.tags t WHERE t.name = 'sale'`): collection nạp về sẽ thiếu phần tử, Persistence Context lưu trạng thái sai.
- Sort trên biểu thức (hàm, phép tính) cần `JpaSort.unsafe("LENGTH(p.name)")`; `Sort.by` chỉ nhận tên thuộc tính.
- Text block `"""` (Java 15+) giúp JPQL nhiều dòng dễ đọc và dễ so sánh trong code review.

## Phần 4: Native Query (SQL thuần)

`@Query(nativeQuery = true)` gửi nguyên văn câu SQL xuống CSDL. Dùng nó khi cần tính năng riêng của CSDL hoặc khi đã đo được câu SQL viết tay nhanh hơn rõ rệt; còn lại ưu tiên JPQL.

### 4.1 Interface Projection cho kết quả Native

Spring Data ánh xạ mỗi cột theo alias vào getter cùng tên. PostgreSQL tự chuyển alias không có ngoặc kép về chữ thường (`productId` thành `productid`), nên đặt alias trong ngoặc kép: `AS "productId"`.

```java
package com.example.shop.projection;

import java.math.BigDecimal;

// Spring tạo proxy cài đặt interface này lúc chạy; mỗi getter ứng với một alias cột
public interface TopProductView {

    Long getProductId();

    String getProductName();

    String getCategoryName();

    BigDecimal getPrice();

    Long getRankInCategory(); // ROW_NUMBER() trả về BIGINT
}
```

```java
package com.example.shop.projection;

import java.math.BigDecimal;

public interface MonthlyRevenueView {

    Long getUserId();

    String getEmail();

    String getRevenueMonth(); // Định dạng 'YYYY-MM' bằng TO_CHAR để tránh lệ thuộc kiểu timestamp của driver

    BigDecimal getRevenue();

    BigDecimal getCumulativeRevenue();
}
```

### 4.2 ProductNativeRepository: Subquery, Window Function, phân trang

```java
package com.example.shop.repository;

import com.example.shop.entity.Product;
import com.example.shop.projection.TopProductView;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductNativeRepository extends JpaRepository<Product, Long> {

    // CORRELATED SUBQUERY: sản phẩm đắt hơn giá trung bình của chính danh mục đó
    // SELECT p.* trả đủ cột nên Hibernate map được thành Entity Product được quản lý
    @Query(value = """
            SELECT p.*
            FROM products p
            WHERE p.price > (
                SELECT AVG(p2.price)
                FROM products p2
                WHERE p2.category_id = p.category_id
            )
            ORDER BY p.category_id, p.price DESC
            """, nativeQuery = true)
    List<Product> findAboveCategoryAveragePrice();

    // WINDOW FUNCTION: top N sản phẩm đắt nhất MỖI danh mục
    // ROW_NUMBER() đánh số lại từ 1 trong từng PARTITION (từng category_id)
    @Query(value = """
            SELECT ranked.product_id   AS "productId",
                   ranked.product_name AS "productName",
                   ranked.category_name AS "categoryName",
                   ranked.price        AS "price",
                   ranked.rank_no      AS "rankInCategory"
            FROM (
                SELECT p.id    AS product_id,
                       p.name  AS product_name,
                       c.name  AS category_name,
                       p.price AS price,
                       ROW_NUMBER() OVER (PARTITION BY p.category_id ORDER BY p.price DESC, p.id) AS rank_no
                FROM products p
                JOIN categories c ON c.id = p.category_id
            ) ranked
            WHERE ranked.rank_no <= :topN
            ORDER BY ranked.category_name, ranked.rank_no
            """, nativeQuery = true)
    List<TopProductView> findTopProductsPerCategory(@Param("topN") int topN);

    // PHÂN TRANG NATIVE: Spring nối LIMIT/OFFSET; countQuery viết riêng cho chính xác và nhanh
    @Query(
            value = """
                    SELECT p.*
                    FROM products p
                    JOIN product_tags pt ON pt.product_id = p.id
                    JOIN tags t ON t.id = pt.tag_id
                    WHERE t.name = :tagName
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM product_tags pt
                    JOIN tags t ON t.id = pt.tag_id
                    WHERE t.name = :tagName
                    """,
            nativeQuery = true)
    Page<Product> findPageByTagName(@Param("tagName") String tagName, Pageable pageable);
}
```

Với phân trang native, thuộc tính sắp xếp trong `Pageable` phải là tên cột thật (`price`, `created_at`), không phải tên field Java (`createdAt`).

### 4.3 OrderNativeRepository: CTE và @Modifying

```java
package com.example.shop.repository;

import com.example.shop.entity.Order;
import com.example.shop.projection.MonthlyRevenueView;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OrderNativeRepository extends JpaRepository<Order, Long> {

    // CTE (WITH): tính doanh thu theo tháng của từng user, rồi cộng dồn bằng window function SUM() OVER
    @Query(value = """
            WITH monthly_revenue AS (
                SELECT o.user_id,
                       DATE_TRUNC('month', o.created_at) AS revenue_month,
                       SUM(o.total_amount)               AS revenue
                FROM orders o
                WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')
                  AND o.created_at >= :fromDate
                GROUP BY o.user_id, DATE_TRUNC('month', o.created_at)
            )
            SELECT u.id                                   AS "userId",
                   u.email                                AS "email",
                   TO_CHAR(mr.revenue_month, 'YYYY-MM')   AS "revenueMonth",
                   mr.revenue                             AS "revenue",
                   SUM(mr.revenue) OVER (
                       PARTITION BY mr.user_id
                       ORDER BY mr.revenue_month
                   )                                      AS "cumulativeRevenue"
            FROM monthly_revenue mr
            JOIN users u ON u.id = mr.user_id
            ORDER BY u.id, mr.revenue_month
            """, nativeQuery = true)
    List<MonthlyRevenueView> monthlyRevenueSince(@Param("fromDate") LocalDateTime fromDate);

    // CẬP NHẬT HÀNG LOẠT: 1 câu UPDATE thay vì tải N Entity rồi set từng cái
    // flushAutomatically: flush thay đổi đang chờ TRƯỚC khi chạy UPDATE, tránh bị ghi đè
    // clearAutomatically: xóa Persistence Context SAU khi chạy, tránh đọc lại Entity cũ trong bộ nhớ
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE orders
            SET status = 'CANCELLED'
            WHERE status = 'PENDING'
              AND created_at < :threshold
            """, nativeQuery = true)
    int cancelPendingOrdersOlderThan(@Param("threshold") LocalDateTime threshold); // Trả số dòng bị ảnh hưởng

    // XÓA HÀNG LOẠT: đơn đã hủy và quá hạn lưu trữ
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            DELETE FROM orders
            WHERE status = 'CANCELLED'
              AND created_at < :threshold
              AND total_amount < :maxAmount
            """, nativeQuery = true)
    int purgeCancelledOrders(@Param("threshold") LocalDateTime threshold,
                             @Param("maxAmount") BigDecimal maxAmount);
}
```

```java
package com.example.shop.repository;

import com.example.shop.entity.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    // Xóa tag không còn gắn với sản phẩm nào: NOT EXISTS trên bảng nối
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = """
            DELETE FROM tags t
            WHERE NOT EXISTS (
                SELECT 1 FROM product_tags pt WHERE pt.tag_id = t.id
            )
            """, nativeQuery = true)
    int deleteUnusedTags();
}
```

### 4.4 Gọi từ Service

```java
package com.example.shop.service;

import com.example.shop.projection.MonthlyRevenueView;
import com.example.shop.projection.TopProductView;
import com.example.shop.repository.OrderNativeRepository;
import com.example.shop.repository.ProductNativeRepository;
import com.example.shop.repository.TagRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final ProductNativeRepository productNativeRepository;
    private final OrderNativeRepository orderNativeRepository;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<TopProductView> topThreePerCategory() {
        return productNativeRepository.findTopProductsPerCategory(3);
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueView> revenueLastSixMonths() {
        return orderNativeRepository.monthlyRevenueSince(LocalDateTime.now().minusMonths(6));
    }

    // Transaction ở Service bao cả hai thao tác: một bên lỗi thì cả hai rollback
    @Transactional
    public void nightlyCleanup() {
        int cancelled = orderNativeRepository.cancelPendingOrdersOlderThan(LocalDateTime.now().minusDays(3));
        int deletedTags = tagRepository.deleteUnusedTags();
        log.info("Đã hủy {} đơn PENDING quá hạn, xóa {} tag không dùng", cancelled, deletedTags);
    }
}
```

### 4.5 @Modifying và @Transactional

- `@Modifying` bắt buộc cho mọi `@Query` chứa UPDATE, DELETE hoặc INSERT; thiếu nó Spring gọi `getResultList()` và ném `InvalidDataAccessApiUsageException`.
- Phương thức ghi cần transaction. Các method mặc định của `SimpleJpaRepository` đã có `@Transactional`, method tự khai báo thì chưa, nên phải đặt `@Transactional` trên method hoặc trên Service gọi nó.
- Câu UPDATE/DELETE hàng loạt bỏ qua Persistence Context: không chạy `@PrePersist`/`@PreUpdate`, không tăng `@Version`, không áp dụng cascade hay `orphanRemoval`.
- Câu lệnh native UPDATE còn làm Hibernate vô hiệu hóa toàn bộ second-level cache vì không biết bảng nào bị đổi. Có thể khai báo bảng bị ảnh hưởng qua `NativeQuery#addSynchronizedEntityClass` khi dùng `EntityManager` (Phần 5).
- JPQL cũng có bulk update (`UPDATE Product p SET p.price = p.price * :factor WHERE p.category.id = :id`) và nên được ưu tiên khi không cần cú pháp đặc thù của CSDL.

### 4.6 Khi nào nên và không nên dùng Native Query

| Nên dùng | Không nên dùng |
| --- | --- |
| Window function, CTE đệ quy, `LATERAL`, `ON CONFLICT`, JSONB, full-text search | CRUD và truy vấn lọc thông thường mà JPQL hoặc derived query làm được |
| Báo cáo tổng hợp nặng, kết quả trả về DTO chỉ đọc | Ứng dụng cần chạy trên nhiều CSDL (PostgreSQL, MySQL, Oracle) |
| Câu SQL đã được DBA tối ưu, cần hint hoặc index cụ thể | Khi cần Entity được quản lý để sửa rồi lưu qua dirty checking |
| Thao tác hàng loạt trên hàng triệu dòng | Khi muốn dùng second-level cache và query cache của Hibernate |

Cái giá phải trả của Native Query:

- Mất tính di động: `DATE_TRUNC`, `TO_CHAR`, `NULLS LAST` là cú pháp PostgreSQL.
- Mất kiểm tra lúc khởi động: sai tên cột chỉ lộ ra khi câu lệnh chạy, nên mỗi native query cần một integration test (Testcontainers).
- Đổi tên cột trong Entity không tự cập nhật câu SQL; refactor bằng IDE không chạm tới chuỗi SQL.
- Hibernate không biết kết quả phụ thuộc bảng nào, nên không cache được và phải xóa cache khi ghi.

Trước khi viết native, hãy thử HQL của Hibernate 6: nó đã hỗ trợ CTE (`WITH`), window function và `LIMIT` trong subquery mà vẫn giữ tính di động.

## Phần 5: Truy vấn bằng EntityManager thuần (JPA Standard)

`EntityManager` là API gốc của JPA mà mọi Repository của Spring Data đều gọi bên dưới. Dùng trực tiếp nó khi cần truy vấn động, kiểm soát chi tiết Persistence Context, hoặc xử lý dữ liệu lớn theo lô.

### 5.1 Vòng đời Entity trong Persistence Context

| Trạng thái | Ý nghĩa | Chuyển sang bằng |
| --- | --- | --- |
| Transient | Object mới tạo bằng `new`, Hibernate chưa biết tới | — |
| Managed | Được Persistence Context theo dõi; mọi thay đổi tự động thành UPDATE khi flush (dirty checking) | `persist()`, `find()`, `merge()` (giá trị trả về), kết quả truy vấn |
| Detached | Từng được quản lý nhưng context đã đóng hoặc bị tách | `detach()`, `clear()`, `close()`, kết thúc transaction |
| Removed | Đã đánh dấu xóa, DELETE chạy khi flush | `remove()` |

Các thao tác chính:

- `persist(entity)`: Transient thành Managed. INSERT chạy khi flush (riêng `GenerationType.IDENTITY` chạy ngay để lấy id).
- `merge(entity)`: sao chép trạng thái của object Detached vào một bản Managed và trả bản đó về. Object truyền vào vẫn Detached.
- `flush()`: đẩy toàn bộ SQL đang chờ xuống CSDL nhưng chưa commit; transaction rollback thì vẫn mất.
- `clear()`: tách mọi Entity khỏi context. Thay đổi chưa flush sẽ bị bỏ qua.
- `refresh(entity)`: đọc lại từ CSDL, ghi đè thay đổi trong bộ nhớ.
- `getReference(Class, id)`: trả proxy mà không SELECT; dùng khi chỉ cần id để gán khóa ngoại.

### 5.2 Đối tượng tiêu chí tìm kiếm và DTO

```java
package com.example.shop.dto;

import java.math.BigDecimal;
import java.util.Set;

// Mọi trường đều tùy chọn: null nghĩa là người dùng không lọc theo trường đó
public record ProductSearchCriteria(
        String keyword,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Set<String> tagNames,
        Boolean inStockOnly) {
}
```

```java
package com.example.shop.dto;

public record TagUsageDto(String tagName, Long productCount) {
}
```

### 5.3 Custom Repository Fragment

Spring Data tìm class có tên `<tên interface fragment>Impl` và ghép các method của nó vào `ProductRepository` (Phần 2). Nhờ vậy service chỉ cần một Repository nhưng vẫn có cả derived query lẫn code `EntityManager`.

```java
package com.example.shop.repository;

import com.example.shop.dto.ProductSearchCriteria;
import com.example.shop.dto.ProductSummaryDto;
import com.example.shop.dto.TagUsageDto;
import com.example.shop.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductCustomRepository {

    List<Product> findByCategoryNameUsingJpql(String categoryName, int maxResults);

    List<ProductSummaryDto> findSummariesUsingJpql(BigDecimal minPrice);

    List<TagUsageDto> countProductsPerTag();

    List<Product> findCreatedWithinDaysNative(int days);

    Page<Product> search(ProductSearchCriteria criteria, Pageable pageable);
}
```

```java
package com.example.shop.repository;

import com.example.shop.dto.ProductSearchCriteria;
import com.example.shop.dto.ProductSummaryDto;
import com.example.shop.dto.TagUsageDto;
import com.example.shop.entity.Category;
import com.example.shop.entity.Product;
import com.example.shop.entity.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// Lưu ý: Order ở đây là jakarta.persistence.criteria.Order (thứ tự sắp xếp),
// không phải Entity com.example.shop.entity.Order, nên không import Entity đó vào class này
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    // Chỉ cho phép sắp xếp theo các field này: chặn tham số sort tùy ý từ client
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("name", "price", "createdAt", "stockQuantity");

    // Spring tiêm một proxy thread-safe, tự gắn với transaction hiện tại của từng luồng
    @PersistenceContext
    private EntityManager entityManager;

    // ===== (A) createQuery với JPQL =====
    @Override
    public List<Product> findByCategoryNameUsingJpql(String categoryName, int maxResults) {
        TypedQuery<Product> query = entityManager.createQuery("""
                SELECT p FROM Product p
                JOIN FETCH p.category c
                WHERE c.name = :categoryName
                ORDER BY p.createdAt DESC
                """, Product.class);
        query.setParameter("categoryName", categoryName);
        query.setMaxResults(maxResults);                   // Sinh LIMIT/FETCH FIRST ở mức SQL
        query.setHint("org.hibernate.readOnly", true);     // Không lưu snapshot cho dirty checking
        return query.getResultList();
    }

    // ===== (A) createQuery trả DTO =====
    @Override
    public List<ProductSummaryDto> findSummariesUsingJpql(BigDecimal minPrice) {
        return entityManager.createQuery("""
                        SELECT new com.example.shop.dto.ProductSummaryDto(p.id, p.name, p.price, c.name)
                        FROM Product p JOIN p.category c
                        WHERE p.price >= :minPrice
                        ORDER BY p.price DESC
                        """, ProductSummaryDto.class)
                .setParameter("minPrice", minPrice)
                .getResultList();
    }

    // ===== (B) createNativeQuery trả Object[] và tự map =====
    @Override
    @SuppressWarnings("unchecked")
    public List<TagUsageDto> countProductsPerTag() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT t.name, COUNT(pt.product_id)
                FROM tags t
                LEFT JOIN product_tags pt ON pt.tag_id = t.id
                GROUP BY t.id, t.name
                ORDER BY COUNT(pt.product_id) DESC
                """).getResultList();
        List<TagUsageDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String tagName = (String) row[0];
            // Kiểu số phụ thuộc driver (Long, BigInteger, BigDecimal): ép qua Number cho an toàn
            long productCount = ((Number) row[1]).longValue();
            result.add(new TagUsageDto(tagName, productCount));
        }
        return result;
    }

    // ===== (B) createNativeQuery map thẳng vào Entity =====
    @Override
    @SuppressWarnings("unchecked")
    public List<Product> findCreatedWithinDaysNative(int days) {
        // Tham số tên (:days) trong native query là mở rộng của Hibernate; đặc tả JPA chỉ bảo đảm ?1
        return entityManager.createNativeQuery("""
                        SELECT * FROM products
                        WHERE created_at >= NOW() - (:days * INTERVAL '1 day')
                        ORDER BY created_at DESC
                        """, Product.class)
                .setParameter("days", days)
                .getResultList();
    }

    // ===== (C) Criteria API: truy vấn động với bộ lọc tùy chọn =====
    @Override
    public Page<Product> search(ProductSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        // Câu lấy dữ liệu
        CriteriaQuery<Product> dataQuery = criteriaBuilder.createQuery(Product.class);
        Root<Product> productRoot = dataQuery.from(Product.class);       // FROM Product p
        productRoot.fetch("category", JoinType.INNER);                   // JOIN FETCH p.category (N-1, an toàn khi phân trang)
        List<Predicate> dataPredicates = buildPredicates(criteriaBuilder, dataQuery, productRoot, criteria);
        dataQuery.select(productRoot)
                .where(dataPredicates.toArray(new Predicate[0]))         // Các Predicate nối bằng AND
                .orderBy(buildOrders(criteriaBuilder, productRoot, pageable.getSort()));

        TypedQuery<Product> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());           // OFFSET
        typedQuery.setMaxResults(pageable.getPageSize());                // LIMIT
        List<Product> content = typedQuery.getResultList();

        // Câu đếm: Root riêng, KHÔNG fetch (COUNT với fetch join là không hợp lệ)
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<Product> countRoot = countQuery.from(Product.class);
        List<Predicate> countPredicates = buildPredicates(criteriaBuilder, countQuery, countRoot, criteria);
        countQuery.select(criteriaBuilder.count(countRoot))
                .where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    // Chỉ thêm Predicate cho tiêu chí người dùng thực sự nhập
    private List<Predicate> buildPredicates(CriteriaBuilder criteriaBuilder,
                                            CriteriaQuery<?> query,
                                            Root<Product> root,
                                            ProductSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
            // LOWER(p.name) LIKE :pattern; giá trị vẫn được bind thành tham số, không nối chuỗi SQL
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("name")), pattern));
        }
        if (criteria.categoryId() != null) {
            // So sánh trực tiếp cột FK category_id, không cần JOIN thêm
            predicates.add(criteriaBuilder.equal(
                    root.<Category>get("category").<Long>get("id"), criteria.categoryId()));
        }
        if (criteria.minPrice() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<BigDecimal>get("price"), criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<BigDecimal>get("price"), criteria.maxPrice()));
        }
        if (Boolean.TRUE.equals(criteria.inStockOnly())) {
            predicates.add(criteriaBuilder.greaterThan(root.<Integer>get("stockQuantity"), 0));
        }
        if (criteria.tagNames() != null && !criteria.tagNames().isEmpty()) {
            // Subquery thay vì JOIN tags: không nhân bản dòng Product, không cần DISTINCT, phân trang đúng
            // p.id IN (SELECT p2.id FROM Product p2 JOIN p2.tags t WHERE t.name IN :tagNames)
            Subquery<Long> tagSubquery = query.subquery(Long.class);
            Root<Product> subRoot = tagSubquery.from(Product.class);
            Join<Product, Tag> tagJoin = subRoot.join("tags");
            tagSubquery.select(subRoot.<Long>get("id"))
                    .where(tagJoin.<String>get("name").in(criteria.tagNames()));
            predicates.add(root.<Long>get("id").in(tagSubquery));
        }
        return predicates;
    }

    // Chuyển Sort của Spring Data thành danh sách Order của Criteria API
    private List<Order> buildOrders(CriteriaBuilder criteriaBuilder, Root<Product> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order sortOrder : sort) {
            if (!SORTABLE_PROPERTIES.contains(sortOrder.getProperty())) {
                throw new IllegalArgumentException("Không hỗ trợ sắp xếp theo: " + sortOrder.getProperty());
            }
            Path<Object> path = root.get(sortOrder.getProperty());
            orders.add(sortOrder.isAscending() ? criteriaBuilder.asc(path) : criteriaBuilder.desc(path));
        }
        if (orders.isEmpty()) {
            orders.add(criteriaBuilder.desc(root.get("createdAt")));
        }
        // Thêm id làm tie-breaker: thứ tự ổn định thì các trang không bị trùng hoặc sót bản ghi
        orders.add(criteriaBuilder.asc(root.get("id")));
        return orders;
    }
}
```

Criteria API dùng chuỗi `"price"` nên sai tên field chỉ lộ ra lúc chạy. Thêm dependency `hibernate-jpamodelgen` để sinh Static Metamodel (`Product_.price`) và có kiểm tra kiểu lúc biên dịch. Với Spring Data, `Specification<Product>` + `JpaSpecificationExecutor` bọc chính Criteria API này và tự xử lý câu COUNT.

Gọi từ Service:

```java
package com.example.shop.service;

import com.example.shop.dto.ProductSearchCriteria;
import com.example.shop.entity.Product;
import com.example.shop.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchService {

    private final ProductRepository productRepository; // Chứa cả method của ProductCustomRepositoryImpl

    public Page<Product> searchLaptopsOnSale() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(
                "laptop",                       // keyword
                null,                           // categoryId: bỏ qua
                new BigDecimal("10000000"),     // minPrice
                new BigDecimal("40000000"),     // maxPrice
                Set.of("sale", "new-arrival"),  // tagNames
                true);                          // inStockOnly
        return productRepository.search(criteria, PageRequest.of(0, 20, Sort.by("price").ascending()));
    }
}
```

### 5.4 Quản lý Transaction và Persistence Context

```java
package com.example.shop.service;

import com.example.shop.entity.Category;
import com.example.shop.entity.Product;
import com.example.shop.entity.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductAdminService {

    private static final int BATCH_SIZE = 30; // Khớp hibernate.jdbc.batch_size

    @PersistenceContext
    private EntityManager entityManager;

    // persist + getReference
    @Transactional
    public Product createProduct(String name, BigDecimal price, int stockQuantity, Long categoryId) {
        // getReference trả proxy, không SELECT categories; chỉ cần id để ghi cột category_id
        Category categoryReference = entityManager.getReference(Category.class, categoryId);
        Product product = Product.builder()
                .name(name)
                .price(price)
                .stockQuantity(stockQuantity)
                .category(categoryReference)
                .build();
        entityManager.persist(product); // Transient -> Managed
        return product;                 // Đã có id vì IDENTITY buộc INSERT chạy ngay
    }

    // Dirty checking: không gọi save, không gọi merge
    @Transactional
    public void changePrice(Long productId, BigDecimal newPrice) {
        Product product = entityManager.find(Product.class, productId); // Managed
        if (product == null) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm " + productId);
        }
        product.setPrice(newPrice); // Hibernate so với snapshot lúc nạp và sinh UPDATE khi commit
    }

    // merge: nhận object Detached (ví dụ dựng từ request body) và đưa vào context
    @Transactional
    public Product updateFromDetached(Product detachedProduct) {
        Product managedProduct = entityManager.merge(detachedProduct); // SELECT nếu chưa có trong context, rồi copy field
        // detachedProduct vẫn Detached; mọi thay đổi tiếp theo phải làm trên managedProduct
        managedProduct.setStockQuantity(Math.max(0, managedProduct.getStockQuantity()));
        return managedProduct;
    }

    // flush + clear: nhập hàng loạt mà bộ nhớ không phình
    @Transactional
    public int importProducts(List<Product> products, Long categoryId) {
        Category categoryReference = entityManager.getReference(Category.class, categoryId);
        int imported = 0;
        for (Product product : products) {
            product.setCategory(categoryReference);
            entityManager.persist(product);
            imported++;
            if (imported % BATCH_SIZE == 0) {
                entityManager.flush(); // Đẩy các INSERT đang chờ xuống CSDL (chưa commit)
                entityManager.clear(); // Bỏ toàn bộ Entity khỏi context, giải phóng bộ nhớ
                // Proxy cũ đã Detached sau clear: lấy lại reference mới cho các bản ghi tiếp theo
                categoryReference = entityManager.getReference(Category.class, categoryId);
            }
        }
        entityManager.flush();
        entityManager.clear();
        return imported;
    }

    // contains, detach, refresh
    @Transactional
    public BigDecimal reloadPriceDiscardingChanges(Long productId) {
        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm " + productId);
        }
        product.setPrice(BigDecimal.ZERO);   // Thay đổi trong bộ nhớ
        entityManager.refresh(product);      // SELECT lại, giá trị 0 bị ghi đè bằng giá trong DB
        entityManager.detach(product);       // Từ đây entityManager.contains(product) == false
        product.setPrice(BigDecimal.ONE);    // Detached: thay đổi này KHÔNG được ghi xuống DB
        return entityManager.find(Product.class, productId).getPrice(); // SELECT mới, trả giá gốc
    }

    // remove: gỡ quan hệ N-N trước để hai phía trong bộ nhớ nhất quán
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            return;
        }
        for (Tag tag : new HashSet<>(product.getTags())) { // Duyệt bản sao để tránh ConcurrentModificationException
            product.removeTag(tag);
        }
        entityManager.remove(product); // DELETE product_tags và products khi flush
    }
}
```

Với `GenerationType.IDENTITY`, Hibernate phải INSERT ngay từng dòng để lấy id nên tắt JDBC batch insert. Khi nhập dữ liệu lớn, đổi sang `GenerationType.SEQUENCE` với `allocationSize = 50` để batch hoạt động.

### 5.5 Spring Data Repository hay EntityManager?

| Tiêu chí | Spring Data JPA Repository | EntityManager trực tiếp |
| --- | --- | --- |
| Lượng code | Rất ít: interface + tên method hoặc `@Query` | Nhiều: tự viết query, tham số, map kết quả |
| Truy vấn tĩnh | Rất phù hợp | Thừa thãi |
| Truy vấn động (bộ lọc tùy chọn) | Cần `Specification` hoặc Querydsl | Criteria API trực tiếp, kiểm soát hoàn toàn |
| Phân trang | Tự động sinh câu COUNT | Tự viết câu COUNT |
| Kiểm soát Persistence Context | Hạn chế (`save` gọi `persist` hoặc `merge` tùy `isNew`) | Đầy đủ: `flush`, `clear`, `detach`, `refresh`, `getReference` |
| Xử lý theo lô lớn | `saveAll` không clear context, dễ hết bộ nhớ | `flush` + `clear` theo chu kỳ |
| Hint, lock, timeout | Qua `@QueryHints`, `@Lock` | API trực tiếp: `setHint`, `setLockMode` |
| Kiểm thử | Dễ mock interface | Cần integration test |

Khuyến nghị: Repository là mặc định cho khoảng 90% truy vấn. Dùng `EntityManager` trong một Custom Repository Fragment cho phần còn lại, để tầng service vẫn chỉ phụ thuộc vào một interface Repository.

## Tổng kết: chọn cách truy vấn và checklist

Thứ tự ưu tiên khi viết một truy vấn mới: Derived Query, rồi `@Query` JPQL, rồi Criteria/Specification cho bộ lọc động, cuối cùng mới đến Native Query.

| Tình huống | Công cụ phù hợp |
| --- | --- |
| Lọc 1-3 điều kiện cố định, có thể phân trang | Derived Query |
| Cần JOIN FETCH, GROUP BY, DTO, điều kiện phức tạp | `@Query` JPQL |
| Bộ lọc tùy chọn do người dùng nhập | Criteria API / `Specification` |
| Window function, CTE, cú pháp riêng của CSDL, báo cáo nặng | Native Query + interface projection |
| Xử lý theo lô, kiểm soát flush/clear, lock, hint | `EntityManager` trong Custom Repository Fragment |

Checklist trước khi đưa code JPA lên production:

- [ ] Mọi `@ManyToOne` và `@OneToOne` đã khai báo `FetchType.LAZY`.
- [ ] Quan hệ hai chiều có `mappedBy` đúng phía và helper method đồng bộ hai đầu.
- [ ] Không dùng `@Data`; `toString` loại trừ field quan hệ; `equals`/`hashCode` dựa trên `id`.
- [ ] `@ManyToMany` dùng `Set` và không cascade `REMOVE`.
- [ ] `spring.jpa.open-in-view=false`; mỗi use case tự nạp đủ dữ liệu bằng JOIN FETCH, `@EntityGraph` hoặc DTO.
- [ ] Đã bật log SQL khi phát triển và kiểm tra không còn N+1 cho các màn hình danh sách.
- [ ] Không phân trang trên JOIN FETCH collection; dùng kỹ thuật hai bước.
- [ ] Màn hình chỉ đọc trả DTO và chạy trong `@Transactional(readOnly = true)`.
- [ ] Mọi `@Modifying` có `@Transactional` và `clearAutomatically = true`.
- [ ] Mỗi Native Query có integration test chạy trên đúng loại CSDL (Testcontainers).
- [ ] Tham số truy vấn luôn được bind, không nối chuỗi; thuộc tính sort từ client được kiểm tra theo danh sách cho phép.
- [ ] Nhập dữ liệu lớn dùng `SEQUENCE`, `jdbc.batch_size` và `flush`/`clear` theo chu kỳ.
