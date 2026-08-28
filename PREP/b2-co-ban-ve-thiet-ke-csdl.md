# Buổi 2: Cơ bản về thiết kế cơ sở dữ liệu 

---

## 1. Lý thuyết cơ bản về thiết kế cơ sở dữ liệu

Thiết kế CSDL là quá trình chuyển đổi các yêu cầu thực tế của ứng dụng thành một mô hình dữ liệu có cấu trúc, tối ưu, đảm bảo Tính toàn vẹn dữ liệu (Data Integrity) và Hiệu năng (Performance).

### Các giai đoạn thiết kế CSDL

#### 1. Thu thập và phân tích yêu cầu:

- Xác định mục đích sử dụng hệ thống và đối tượng tương tác.
- Liệt kê các loại dữ liệu cần lưu trữ và các báo cáo cần xuất.
- Làm rõ quy tắc nghiệp vụ từ phía người dùng.

#### 2. Thiết kế khái niệm (Conceptual Design):

- Xây dựng mô hình dữ liệu độc lập với hệ quản trị cơ sở dữ liệu (DBMS) cụ thể.
- Xác định các thực thể chính (Entities) và thuộc tính của chúng.
- Xác định mối quan hệ (Relationships) giữa các thực thể.
- Vẽ sơ đồ E-R.

#### 3. Thiết kế logic (Logical Design)

- Chuyển đổi mô hình khái niệm sang mô hình dữ liệu của DBMS (như mô hình quan hệ bảng).
- Định nghĩa các bảng, khóa chính, khóa ngoại.
- Áp dụng các quy tắc chuẩn hóa dữ liệu (1NF, 2NF, 3NF...) để tránh dư thừa và bất nhất.

#### 4. Thiết kế vật lý (Physical Design)

- Lựa chọn hệ quản trị cơ sở dữ liệu cụ thể (MySQL, PostgreSQL, SQL Server...).
- Định nghĩa kiểu dữ liệu chi tiết cho từng cột và tối ưu hóa lưu trữ.
- Tạo chỉ mục (Indexing) cho các trường hay tìm kiếm để tăng tốc độ truy vấn.
- Thiết lập phân quyền, chính sách bảo mật và sao lưu dữ liệu.


## 2. Lược đồ quan hệ Entity-Relationship (E-R Diagram)

Lược đồ E-R biểu diễn cấu trúc dữ liệu ở mức khái niệm thông qua các đối tượng và mối quan hệ giữa chúng.

### 2.1. Thành phần Cốt lõi của ERD:

#### 2.1.1 Thực thể (Entity) & Tập thực thể (Entity Set):

- Thực thể (Entity): Một đối tượng cụ thể hoặc trừu tượng trong thế giới thực cần quản lý. 

- Tập thực thể (Entity Set): Tập hợp các thực thể cùng loại 

- Phân loại thực thể:

  - Thực thể mạnh (Strong Entity): Tự định danh bằng thuộc tính khóa của chính nó. Ký hiệu: Hình chữ nhật đơn.

  - Thực thể yếu (Weak Entity): Không thể tự định danh nếu thiếu thực thể chủ (Owner Entity). Ký hiệu: Hình chữ nhật viền đôi.

#### 2.1.2 Thuộc tính (Attribute):

Mô tả các đặc trưng của một thực thể. Ký hiệu: Hình Elip / Bầu dục.

- Thuộc tính đơn (Simple Attribute): Không thể chia nhỏ hơn (GiaBia, NamXuatBan).
- Thuộc tính khóa (Key Attribute): Định danh duy nhất thực thể. Ký hiệu: Viết hoa và gạch chân nét liền (MaBanDoc).
- Thuộc tính phức hợp (Composite Attribute): Cấu thành từ nhiều thuộc tính nhỏ hơn (HoTen $\rightarrow$ Ho, Ten).
- Thuộc tính đa trị (Multivalued Attribute): Có thể chứa nhiều giá trị đồng thời cho một thực thể (ví dụ: một người có nhiều SoDienThoai). Ký hiệu: Hình elip viền đôi.
- Thuộc tính dẫn xuất (Derived Attribute): Giá trị tính toán ra từ thuộc tính khác (ví dụ: Tuoi tính từ NgaySinh). Ký hiệu: Hình elip nét đứt.

#### 2.1.3 Mối liên kết (Relationship) & Bản lượng (Cardinality)

- Sự gắn kết giữa 2 hoặc nhiều tập thực thể. Ký hiệu: Hình thoi.
- Mối liên kết xác định (Identifying Relationship): Liên kết thực thể yếu với thực thể chủ. Ký hiệu: Hình thoi viền đôi.
- Bản lượng liên kết (Cardinality Ratio): 
  - $1 - 1$: 1 A kết nối tối đa 1 B (ví dụ: BanDoc — TheThuVien).
  - $1 - N$: 1 A kết nối nhiều B, nhưng 1 B chỉ thuộc 1 A (ví dụ: LopHoc — SinhVien).
  - $N - M$: 1 A kết nối nhiều B và ngược lại (ví dụ: BanDoc — Sach).

### 2.2. Các Ký hiệu Chuẩn trong Vẽ sơ đồ ERD

| Thành phần | Ký hiệu hình học | Chi tiết |
| --- | --- | --- |
| **Thực thể mạnh** | Hình chữ nhật nét đơn | Ví dụ: `BanDoc`, `Sach` |
| **Thực thể yếu** | Hình chữ nhật nét đôi | Ví dụ: `ChiTietPhieuMuon` |
| **Mối liên kết thường** | Hình thoi nét đơn | Ví dụ: `Muon`, `Thuoc` |
| **Mối liên kết xác định** | Hình thoi nét đôi | Nối Thực thể chủ $\rightarrow$ Thực thể yếu |
| **Thuộc tính đơn** | Hình elip nét đơn | Ví dụ: `TenSach` |
| **Thuộc tính khóa** | Hình elip gạch chân | Ví dụ: `MaBanDoc` |
| **Thuộc tính đa trị** | Hình elip nét đôi | Ví dụ: `SoDienThoai` |
| **Thuộc tính dẫn xuất** | Hình elip nét đứt | Ví dụ: `SoTienPhat` |
| **Tham gia toàn bộ** | Đường nối kép ($=$) | Mọi thực thể bắt buộc phải tham gia liên kết |

---

## 3. Mô hình dữ liệu Quan hệ (Relational Model)

### 3.1. Các thuật ngữ cốt lõi trong Mô hình Quan hệ

| Thuật ngữ Ý niệm (ERD) | Thuật ngữ Quan hệ (Relational Model) | Thuật ngữ SQL Thực tế |
| --- | --- | --- |
| **Thực thể** *(Entity)* | Quan hệ *(Relation)* | Bảng *(Table)* |
| **Thuộc tính** *(Attribute)* | Thuộc tính *(Attribute)* | Cột *(Column / Field)* |
| **Thể hiện thực thể** *(Entity Instance)* | Bộ *(Tuple)* | Dòng / Bản ghi *(Row / Record)* |
| **Tập giá trị hợp lệ** | Miền giá trị *(Domain)* | Kiểu dữ liệu & Ràng buộc *(Data Type & Constraint)* |
| **Thuộc tính khóa** | Khóa chính *(Primary Key - PK)* | Primary Key Constraint |

---

### 3.2. 8 Quy tắc chuyển đổi chuẩn từ ERD sang Mô hình Quan hệ

| STT | Loại thành phần trong ERD | Quy tắc chuyển đổi sang Relational Schema |
| --- | --- | --- |
| **1** | **Thực thể mạnh** | Tạo Bảng riêng, giữ nguyên Khóa chính (PK). |
| **2** | **Thuộc tính phức hợp** | Phân rã thành các cột đơn thành phần trong cùng một Bảng. |
| **3** | **Thuộc tính đa trị** | Tạo Bảng mới chứa `[PK bảng gốc]` + `[Thuộc tính đa trị]`. |
| **4** | **Thực thể yếu** | Tạo Bảng mới với `PK` = `[PK thực thể chủ]` + `[Khóa bộ phận (Partial Key)]`. |
| **5** | **Quan hệ 1 - 1** | Thêm Khóa ngoại (FK) vào Bảng phía tham gia toàn phần (Total Participation). |
| **6** | **Quan hệ 1 - N** | Thêm Khóa ngoại (FK) vào Bảng ở phía "Nhiều" (N). |
| **7** | **Quan hệ N - N** | Tạo Bảng trung gian mới chứa 2 Khóa ngoại (FK) trỏ về 2 bảng gốc. |
| **8** | **Quan hệ tự thân (1-N)** | Thêm cột Khóa ngoại (FK) trỏ ngược về Khóa chính (PK) của chính Bảng đó. |

--- 

## 4. Lý thuyết Phụ thuộc hàm & Chuẩn hóa dữ liệu (1NF, 2NF, 3NF, BCNF)

### 4.1. Khái niệm cốt lõi về Phụ thuộc hàm (Functional Dependency - FD)

Ký hiệu: $X \rightarrow Y$ ($X$ xác định $Y$, hoặc $Y$ phụ thuộc hàm vào $X$).
Nghĩa là: Nếu biết giá trị của tập thuộc tính $X$, ta chắc chắn xác định được duy nhất giá trị của tập thuộc tính $Y$.

| Khái niệm | Định nghĩa | Ví dụ thực tế |
| --- | --- | --- |
| **FD Đầy đủ** *(Full FD)* | $Y$ phụ thuộc vào toàn bộ $X$, bỏ bất kỳ thuộc tính nào khỏi $X$ thì FD không còn đúng. | `(MaSV, MaMon)` $\rightarrow$ `Diem` |
| **FD Một phần** *(Partial FD)* | $Y$ phụ thuộc vào một phần tập con của $X$ ($X$ là khóa ghép). | `(MaSV, MaMon)` $\rightarrow$ `HoTen` *(Chỉ cần `MaSV` là đủ)* |
| **FD Bắc cầu** *(Transitive FD)* | $X \rightarrow Y$ và $Y \rightarrow Z$ ($Y, Z$ không phải khóa), suy ra $X \rightarrow Z$. | `MaSV` $\rightarrow$ `MaLop` và `MaLop` $\rightarrow$ `TenLop` |

---

### 4.2. Bảng tổng hợp các Dạng chuẩn (Normalization Forms)

| Dạng chuẩn | Vi phạm thường gặp | Điều kiện bắt buộc để đạt chuẩn | Cách xử lý (Giải thuật) |
| --- | --- | --- | --- |
| **1NF** | Ô chứa danh sách/mảng (Đa trị), thuộc tính phức hợp. | Mọi ô dữ liệu phải chứa **giá trị nguyên tố (Atomic Value)**. | Nhân bản dòng cho mỗi giá trị trong danh sách hoặc tách bảng. |
| **2NF** | Tồn tại **Phụ thuộc hàm một phần** vào Khóa chính ghép. | Đạt **1NF** + Mọi thuộc tính không khóa phải **phụ thuộc đầy đủ** vào toàn bộ Khóa chính. | Tách các thuộc tính bị phụ thuộc một phần ra Bảng riêng cùng với phần Khóa đó. |
| **3NF** | Tồn tại **Phụ thuộc hàm bắc cầu** giữa các thuộc tính không khóa. | Đạt **2NF** + Không tồn tại phụ thuộc bắc cầu giữa các thuộc tính không phải khóa. | Tách nhóm thuộc tính có quan hệ phụ thuộc bắc cầu ($Y \rightarrow Z$) ra Bảng riêng. |
| **BCNF** | Khóa ứng viên bị phụ thuộc vào một thuộc tính không phải khóa. | Đạt **3NF** + Với mọi FD $X \rightarrow Y$ phi hiển nhiên, **$X$ phải là một Siêu khóa (Super Key)**. | Tách bảng sao cho vế trái $X$ của FD vi phạm trở thành Khóa chính của bảng mới. |

---

### 4.3. Quy trình 4 bước chuẩn hóa một Bảng thô
```
[ Bảng thô / Unnormalized ]
│  (Đảm bảo dữ liệu nguyên tố / Atomic)
▼
[ 1NF ]
│  (Loại bỏ Phụ thuộc một phần vào PK ghép)
▼
[ 2NF ]
│  (Loại bỏ Phụ thuộc bắc cầu giữa các cột không khóa)
▼
[ 3NF ]
│  (Mọi vế trái X của FD X -> Y đều phải là Siêu khóa)
▼
[ BCNF ]
```
---

### 4.4. So sánh nhanh 3NF và BCNF

* **3NF:** Cho phép $X \rightarrow A$ nếu $X$ là Siêu khóa **hoặc $A$ là thuộc tính khóa** (Primary Attribute).
* **BCNF:** Chặt chẽ hơn 3NF. Bắt buộc $X$ **phải là Siêu khóa** trong mọi trường hợp (không có ngoại lệ cho $A$).