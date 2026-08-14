# Buổi 1: Nhập môn CSDL

---

## 1. Cơ sở dữ liệu(Database) là gì?

Database không đơn thuần là một nơi chứa thông tin mà đây là một tập hợp các dữ liệu có liên quan logic với nhau, được tổ chức và lưu trữ trên các phương tiện lưu trữ một cách có hệ thống, nhằm phục vụ cho việc quản lý, truy xuất và cập nhật dữ liệu bởi một hoặc nhiều người dùng/ứng dụng cùng lúc.

### Đặc điểm của CSDL
**Tính nhất quán & Toàn vẹn (Data Integrity)**:
Ngăn chặn việc dữ liệu bị hỏng hoặc mâu thuẫn (ví dụ: một tài khoản không thể vừa có trạng thái "Đã xóa" lại vừa có giao dịch mới).

**Xử lý đồng thời (Concurrency Control)**:
Nhiều người dùng/ứng dụng có thể đọc và ghi vào CSDL cùng một thời điểm mà không đè lên dữ liệu của nhau.

**An toàn & Bảo mật (Security)**:
Phân quyền chi tiết (ví dụ: Sinh viên chỉ được xem điểm, nhưng Giảng viên có quyền sửa điểm).

**Khả năng mở rộng (Scalability)**:
Cho phép lưu trữ dữ liệu lớn và tối ưu tốc độ tìm kiếm.

### Ví dụ thực tế
- DB của sàn thương mại điện tử:

Ví dụ sử dụng file thông thường để quản lí, gặp trường hợp 1000 người cùng đặt một sản phẩm vào cùng một thời điểm thì sẽ gặp lỗi

Dùng DB Relational (Quan hệ), hệ thống sẽ chia thành các Bảng:
- Bảng `KhachHang`: Lưu thông tin cá nhân.
- Bảng `SanPham`: Lưu giá, số lượng tồn kho.
- Bảng `DonHang`: Liên kết Khách hàng nào đã mua Sản phẩm nào.

## 2. Hệ quản trị CSDL (Database Management System - DBMS) là gì?

DBMS là phần mềm đóng vai trò cầu nối giữa DB và Người dùng/Ứng dụng. Người dùng không bao giờ can thiệp trực tiếp vào file dữ liệu thô trên ổ cứng, mà mọi thao tác (thêm, sửa, xóa, tìm kiếm) đều phải gửi lệnh qua DBMS để nó xử lý.

### Nhiệm vụ cốt lõi của DBMS (Tính chất ACID)
Atomicity (Tính nguyên tố): Giao dịch thực hiện theo nguyên tắc "được ăn cả, ngã về không". (Ví dụ: Chuyển tiền gồm 2 bước: Trừ tiền tài khoản A và Cộng tiền tài khoản B. Nếu đang cộng tiền B mà mất điện, DBMS sẽ tự lùi lại (Rollback), trả lại tiền cho A).

Consistency (Tính nhất quán): Dữ liệu luôn tuân thủ đúng các ràng buộc đã đề ra, không có ngoại lệ.

Isolation (Tính cô lập): Các giao dịch chạy song song không làm ảnh hưởng lẫn nhau.

Durability (Tính bền vững): Khi dữ liệu đã báo thành công (Commit), nó sẽ lưu vĩnh viễn dù sau đó máy chủ có mất điện hay sập.

### Một số hệ quản trị CSDL phổ biến
- **Microsoft SQL Server** (MS SQL Server)
- **MySQL**
- **PostgreSQL**
- **Oracle Database**
- **MongoDB** (NoSQL)
- **SQLite**

---

## 3. Câu lệnh tạo Database, Table trong MS SQL Server

### 3.1. Tạo Database

Cú pháp:
```sql
CREATE DATABASE TenDB;
GO
```

Ví dụ: tạo DB QuanLyNhaHang
```sql
CREATE DATABASE QuanLyNhaHang;
GO
```

Chọn database để làm việc (rất quan trọng, nếu không sẽ tạo bảng nhầm database khác):
```sql
USE QuanLyNhaHang;
GO
```

Xóa database (nếu cần):
```sql
DROP DATABASE QuanLyNhaHang;
GO
```

### 3.2. Tạo Table

Cú pháp tổng quát:
```sql
CREATE TABLE TenBang (
    TenCot1 KieuDuLieu RangBuoc,
    TenCot2 KieuDuLieu RangBuoc,
    ...
);
GO

```

Ví dụ: DB QuanLyNhaHang
Tạo bảng `NhanVien`
```sql
CREATE TABLE NhanVien (
    MaNV INT PRIMARY KEY IDENTITY(1,1), -- Id là duy nhất, gán giá trị đầu = 1, bước nhảy = 1
    HoTen NVARCHAR(100) NOT NULL, -- Họ tên bắt buộc, hỗ trợ tiếng việt
    ChucVu NVARCHAR(50) NOT NULL,-- Chức vụ bắt buộc, hỗ trợ tiếng việt
    Luong DECIMAL(14, 2) CHECK (Luong > 0), -- Lương tối đa 14 chữ số, 2 số sau dấu phẩy, ràng buộc check > 0 
    NgayVaoLam DATE DEFAULT GETDATE() -- Ngày vào làm dạng date
);
GO
```

Tạo bảng `MonAn`

```sql
CREATE TABLE MonAn (
    Id INT PRIMARY KEY IDENTITY(1, 1),
    TenMon NVARCHAR(100) NOT NULL,
    DanhMuc NVARCHAR(50) NOT NULL,
    GiaBan DECIMAL(12, 2) CHECK (GiaBan >= 0),
    TrangThai BIT DEFAULT 1
)
```

Tạo bảng `BanAn`

```sql
CREATE TABLE BanAn (
    MaBan INT PRIMARY KEY IDENTITY(1,1),
    TenBan NVARCHAR(50) NOT NULL,
    TrangThai TINYINT DEFAULT 0 CHECK (TrangThai IN (0, 1, 2)) -- 0: Trống, 1: Có khách, 2: Đặt trước
);
GO
```

Tạo bảng hóa đơn và chi tiết hóa đơn để thể hiện mối quan hệ 1-nhiều và nhiều-nhiều sử dụng khóa ngoại

```sql
CREATE TABLE HoaDon (
    MaHD INT PRIMARY KEY IDENTITY(1,1),
    NgayLap DATETIME DEFAULT GETDATE(),

    -- Khóa ngoại liên kết tới bảng NhanVien
    MaNV INT NOT NULL,
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (MaNV) REFERENCES NhanVien(MaNV),
    
    -- Khóa ngoại liên kết tới bảng BanAn
    MaBan INT NOT NULL,
    CONSTRAINT FK_HoaDon_BanAn FOREIGN KEY (MaBan) REFERENCES BanAn(MaBan),
    
    TongTien DECIMAL(12, 2) DEFAULT 0
);
GO
```

```sql
CREATE TABLE ChiTietHoaDon (
    MaHD INT NOT NULL,
    MaMon INT NOT NULL,
    SoLuong INT CHECK (SoLuong > 0),
    DonGia DECIMAL(10, 2) CHECK (DonGia > 0),

    -- Khóa chính phức hợp (Gồm cả MaHD và MaMon)
    PRIMARY KEY (MaHD, MaMon),
    
    -- Khóa ngoại liên kết tới bảng HoaDon
    CONSTRAINT FK_CTHD_HoaDon FOREIGN KEY (MaHD) REFERENCES HoaDon(MaHD) 
        ON DELETE CASCADE, -- Xóa hóa đơn sẽ tự xóa chi tiết hóa đơn tương ứng
        
    -- Khóa ngoại liên kết tới bảng MonAn
    CONSTRAINT FK_CTHD_MonAn FOREIGN KEY (MaMon) REFERENCES MonAn(MaMon)
);
GO
```

### 3.3. Một số kiểu dữ liệu thường dùng trong SQL Server
| Kiểu dữ liệu | Mô tả |
|---|---|
| `INT` | Số nguyên |
| `FLOAT`, `DECIMAL(m,n)` | Số thực |
| `VARCHAR(n)` | Chuỗi ký tự (không dấu tiếng Việt tốt) |
| `NVARCHAR(n)` | Chuỗi ký tự (hỗ trợ Unicode, nên dùng cho tiếng Việt) |
| `DATE` | Ngày tháng |
| `DATETIME` | Ngày giờ |
| `BIT` | Kiểu boolean (0/1 - đúng/sai) |

### 3.4. Một số ràng buộc (Constraint) thường dùng
| Ràng buộc | Ý nghĩa |
|---|---|
| `PRIMARY KEY` | Khóa chính, xác định duy nhất mỗi dòng dữ liệu |
| `FOREIGN KEY` | Khóa ngoại, liên kết với bảng khác |
| `NOT NULL` | Không được để trống |
| `UNIQUE` | Giá trị không được trùng lặp |
| `DEFAULT` | Giá trị mặc định nếu không nhập |
| `IDENTITY(1,1)` | Tự động tăng giá trị (bắt đầu từ 1, tăng mỗi lần 1) |

### 3.5. Xem cấu trúc và xóa bảng
```sql
-- Xem toàn bộ bảng đang có trong database
SELECT * FROM INFORMATION_SCHEMA.TABLES;

-- Xóa bảng
DROP TABLE MonAn;
```

---