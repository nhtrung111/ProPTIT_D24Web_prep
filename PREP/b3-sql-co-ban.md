# Buổi 3: SQL cơ bản

---

## 1. Các thao tác cơ bản: SELECT, INSERT, UPDATE, DELETE, từ khóa AS, DISTINCT

### 1.1. SELECT

Câu lệnh SELECT được dùng để lấy dữ liệu từ bảng.

Chọn một cột từ trong bảng:  
    `SELECT col FROM table;`

Chọn nhiều cột từ trong bảng:  
    `SELECT col1, col2 FROM table;`

Chọn tất cả các cột trong bảng:  
    `SELECT * FROM table;`

Chọn các giá trị duy nhất(không trùng lặp) từ bảng:
    `SELECT DISTINCT col FROM table;`

### 1.2. INSERT

Câu lệnh INSERT được dùng để thêm bản ghi mới.

Thêm một bản ghi:
```sql
INSERT INTO books (title, author, publish_year, price, description)
VALUES (
'Before I Fall',
'Lauren Oliver',
2010,
14.99,
'A YA novel about Sam Samantha Kingston, a girl who dies in a car crash and relives her last day seven times to fix her mistakes.'
);
```

Thêm nhiều bản ghi:
```sql
INSERT INTO books (title, author, publish_year, price, description)
VALUES 
    ('Before I Fall', 'Lauren Oliver', 2010, 14.99, 'A YA novel about Sam Samantha Kingston, a girl who dies in a car crash and relives her last day seven times to fix her mistakes.'),
    (.......);
```

### 1.3. UPDATE

Câu lệnh UPDATE dùng để cập nhật dữ liệu:

#### Giả sử cuốn sách đang được giảm giá 20%:

UPDATE cho một cuốn sách cụ thể:
```sql
UPDATE books
SET price = price * 0.8
WHERE title = 'Before I Fall';
```

UPDATE cho các cuốn sách thuộc thể loại YA:
```sql
UPDATE books
SET price = price * 0.8
WHERE description LIKE %Young Adult% OR description LIKE %YA%;
```

#### Làm tròn giá sau khi giảm:
```sql
UPDATE books
SET price = ROUND(price * 0.8, 2)
WHERE title = 'Before I Fall';
```

### 1.4. DELETE

Xóa cụ thể từng cuốn sách:
```sql
DELETE FROM books
WHERE title = 'Before I Fall';
```

Xóa cách cuốn sách với điều kiện cụ thể:
```sql
DELETE FROM books
WHERE publish_year < 2000;
```

**Lưu ý: Nếu không dùng WHERE thì mọi dữ liệu trong bảng đều bị xóa;**

### 1.5. AS 

Từ khóa AS được dùng để đặt tên tạm thời cho cột hoặc bảng giúp kết quả trả về dễ đọc hơn hoặc ngắn gọn hơn khi truy vấn.

**Đặt tên lại cho các cột khi hiển thị (Column Alias):**
```sql
SELECT
    title AS TenSach,
    author AS TacGia,
    price AS GiaGiam,
    ROUND(price * 0.8, 2) AS GiaSauGiam20Percent
FROM books;
```

**Đặt tên ngắn gọn cho bảng (Table Alias - rất hữu ích khi dùng JOIN):**
```sql
SELECT b.title, b.author, b.price
FROM books AS b 
WHERE b.publish_year >= 2000
```
### 1.6. DISTINCT

Lấy danh sách không trùng lặp:
```sql
SELECT DISTINCT author
FROM books;
```

Đếm số lượngphaanfn tử riêng biệt:
```sql
SELECT COUNT(DISTINCT author) AS TongSoTacGia
FROM books;
```

## 2. Lọc dữ liệu WHERE vs HAVING

### 2.1 WHERE

WHERE dùng để lọc dữ liệu thô trên từng dòng: Lọc trước khi tính toán hay gom nhóm.

```sql
SELECT title, price
FROM books
WHERE publish_year > 2010 AND price > 10.00;
```

### 2.2 HAVING

HAVING dùng để lọc trên kết quả đã gom nhóm: Đi kèm với GROUP BY để lọc dựa trên kết quả tính toán (COUNT, SUM, AVG).

HAVING
```sql
SELECT author, AVG(price) AS avg_price
FROM books
GROUP BY author
HAVING AVG(price) < 15.00;
```

## 3. Kết hợp bảng và kết quả: JOIN & UNION

### 3.1. Phép JOIN (Kết hợp cột từ nhiều bảng)

INNER JOIN: Lấy danh sách độc giả ĐANG mượn sách
Chỉ hiển thị những bản ghi có sự trùng khớp ở cả 2 bảng (Độc giả phải có trong hệ thống VÀ phải có phiếu mượn).
```sql
SELECT
    r.reader_code,
    r.full_name,
    r.card_barcode,
    b.title,
    bc.barcode,
    bo.borrow_date,
    bd.due_date
FROM borrow_orders bo
    INNER JOIN readers r ON bo.reader_id = r.id
    INNER JOIN borrow_details bd ON bo.id = bd.borrow_order_id
    INNER JOIN book_copies bc ON bd.book_copy_id = bc.id
    INNER JOIN books b ON bc.book_id = b.id
WHERE bd.status = 'BORROWING';
```

### LEFT JOIN (Bảng bên TRÁI làm gốc)

Giữ lại tất cả dữ liệu của bảng đứng trước từ khóa LEFT JOIN.

Lấy danh sách tất cả bạn đọc, kể cả những người chưa từng mượn sách bao giờ.

```sql
SELECT 
    r.reader_code,
    r.full_name,
    bo.id AS borrow_order_id,
    bo.borrow_date
FROM readers r                            
LEFT JOIN borrow_orders bo ON r.id = bo.reader_id; 

```

* **Kết quả:**
* Bạn đọc A (có mượn sách) $\rightarrow$ Trả về thông tin A + Ngày mượn.
* Bạn đọc B (mới tạo tài khoản, chưa mượn) $\rightarrow$ Trả về thông tin B + `borrow_order_id = NULL`, `borrow_date = NULL`.



---

### RIGHT JOIN (Bảng bên PHẢI làm gốc)

Giữ lại tất cả dữ liệu của bảng đứng sau từ khóa `RIGHT JOIN`.

Viết lại câu lệnh trên nhưng dùng `RIGHT JOIN`:

```sql
SELECT 
    r.reader_code,
    r.full_name,
    bo.id AS borrow_order_id,
    bo.borrow_date
FROM borrow_orders bo                      
RIGHT JOIN readers r ON bo.reader_id = r.id;
```

### 2. So sánh: `INNER` vs `LEFT` vs `RIGHT JOIN`

| Loại JOIN | Tập dữ liệu giữ lại | Dữ liệu không khớp xử lý thế nào? |
| --- | --- | --- |
| **`INNER JOIN`** | Chỉ giữ các dòng **khớp ở cả 2 bảng**. | Loại bỏ hoàn toàn. |
| **`LEFT JOIN`** | Giữ **toàn bộ Bảng Trái** + Dòng khớp ở Bảng Phải. | Điền `NULL` vào các cột của Bảng Phải. |
| **`RIGHT JOIN`** | Giữ **toàn bộ Bảng Phải** + Dòng khớp ở Bảng Trái. | Điền `NULL` vào các cột của Bảng Trái. |

---

### 3.2 UNION`

Khác với `JOIN` (gộp dữ liệu theo chiều Ngang - thêm cột), `UNION` dùng để gộp dữ liệu theo chiều Dọc - chồng các bảng kết quả lên nhau.

#### Quy tắc khi dùng `UNION`:

1. Các câu lệnh `SELECT` phải có cùng số lượng cột.
2. Các cột tương ứng ở mỗi `SELECT` phải có cùng (hoặc tương thích) kiểu dữ liệu.
3. Thứ tự các cột phải tương ứng với nhau.

#### Phân biệt `UNION` và `UNION ALL`

| Tiêu chí | `UNION` | `UNION ALL` |
| --- | --- | --- |
| **Xử lý trùng lặp** | Tự động **loại bỏ các dòng trùng lặp** (Distinct). | **Giữ lại tất cả các dòng**, kể cả trùng lặp. |
| **Hiệu năng** | **Chậm hơn** (phải tốn tài nguyên Scan & Sort để lọc trùng). | **Nhanh hơn** (chỉ việc nối thẳng dữ liệu lại). |
| **Khuyên dùng** | Khi chắc chắn cần dữ liệu duy nhất. | Khi biết dữ liệu không trùng hoặc muốn lấy tất cả. |

## 4. Tổng hợp và nhóm dữ liệu: COUNT, SUM, AVG, GROUP BY

Các hàm gom nhóm được dùng để tính toán trên một tập hợp giá trị và trả về một giá trị duy nhất. Khi đi kèm với GROUP BY, dữ liệu sẽ được chia thành các nhóm nhỏ trước khi tính toán.

- COUNT(column): Đếm số dòng có giá trị (bỏ qua NULL). COUNT(*) đếm tất cả các dòng.  
- SUM(column): Tính tổng các giá trị số (bỏ qua NULL).  
- AVG(column): Tính giá trị trung bình các giá trị số (bỏ qua NULL).  

```sql
SELECT
author,
COUNT(id) AS total_books,
AVG(cover_price) AS avg_price
FROM books
WHERE cover_price > 0                   -- Lọc các sách có giá > 0 trước
GROUP BY author                         -- Nhóm theo từng tác giả
HAVING COUNT(id) >= 2                   -- Chỉ lấy tác giả có từ 2 đầu sách trở lên
ORDER BY total_books DESC;
```