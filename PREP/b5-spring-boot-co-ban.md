# Buổi 5: Spring Boot cơ bản

---

## 1. HTTP là gì ?

HTTP (Hypertext Transfer Protocol) là giao thức truyền tải siêu văn bản, là bộ quy tắc chuẩn giúp các máy tính trên mạng Internet có thể nói chuyện và trao đổi dữ liệu với nhau.

Nó chính là nền tảng cốt lõi của toàn bộ mạng www.

### 1.1. HTTP truyền tải những gì?

Ban đầu, HTTP được tạo ra chỉ để lấy các tài nguyên văn bản đơn giản (HTML). Nhưng ngày nay, HTTP truyền tải mọi dạng dữ liệu cấu thành nên một trang web hoặc ứng dụng:

- Văn bản & Mã nguồn: HTML, CSS, JavaScript, JSON, XML.

- Hình ảnh & Đa phương tiện: PNG, JPG, MP4, Audio.

- Tệp tin: PDF, ZIP, các file dữ liệu binary.

Khi mở một trang web, trình duyệt không chỉ gửi 1 request. Nó gửi request đầu tiên để lấy file HTML về, sau đó đọc file HTML thấy có ảnh, CSS, JS... nó tiếp tục gửi các request con nữa để lấy đủ các file đó về ghép lại thành trang web hoàn chỉnh.

### 1.2. Các đặc tính quan trọng của HTTP

Mô hình Request - Response:  
Client luôn là bên chủ động gửi yêu cầu trước, Server tiếp nhận, xử lý và trả về kết quả. Server không tự nhiên gửi dữ liệu nếu Client không hỏi.

Stateless (Không lưu trạng thái):  
Server không nhớ gì về các lần gửi trước. Mỗi Request gửi đi là độc lập hoàn toàn.

Media Independent (Tự do về định dạng dữ liệu):  
HTTP không quan tâm dữ liệu bên trong là gì. Miễn là hai bên báo cho nhau biết kiểu dữ liệu thông qua thông tin Content-Type

### 1.3. Các method trong HTTP

## 1. GET Request

- **Ý nghĩa:** Yêu cầu Server trả về thông tin của tài nguyên. Là phương thức chỉ đọc (Read-only), Safe&Idempotent. 
- **Ánh xạ Spring Boot:** Sử dụng Annotation `@GetMapping` (kết hợp với `@PathVariable` cho đường dẫn hoặc `@RequestParam` cho tham số truy vấn).

### Đề bài

Khách hàng muốn xem chi tiết thông tin của đơn hàng có mã số `101`.

### Code Spring Boot

```java
@GetMapping("/api/don-hang/{id}")
public ResponseEntity<DonHang> xemChiTietDonHang(@PathVariable Long id) {
    DonHang donHang = donHangRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng ID: " + id));

    return ResponseEntity.ok(donHang);
}
```

### HTTP Request gửi đi

```http
GET /api/don-hang/101 HTTP/1.1
Host: quan-ca-phe.com
Accept: application/json

```

### HTTP Response trả về (Output)

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 101,
  "tenMon": "Bạc xỉu",
  "luongDuong": "50%",
  "giaTien": 35000,
  "trangThai": "DA_XAC_NHAN"
}

```


## 2. POST Request
- **Ý nghĩa:** Gửi dữ liệu lên Server để tạo mới một tài nguyên hoặc khởi tạo một quy trình. Không Safe và không Idempotent (gọi 10 lần sẽ tạo ra 10 bản ghi mới khác nhau trong Database). Dữ liệu đính kèm bên trong Request Body.
* **Ánh xạ Spring Boot:** Sử dụng Annotation `@PostMapping` kết hợp `@RequestBody` để chuyển đổi chuỗi JSON gửi lên thành đối tượng Java.

### Đề bài

Khách hàng chọn món trên ứng dụng và bấm tạo một đơn hàng mới.

### Code Spring Boot

```java
@PostMapping("/api/don-hang")
public ResponseEntity<DonHang> taoDonHang(@RequestBody DonHang donHangMoi) {
    // Lưu vào Database và được cấp ID tự động là 102
    donHangMoi.setId(102L);
    donHangMoi.setTrangThai("CHO_PHA_CHE");

    return ResponseEntity.status(HttpStatus.CREATED).body(donHangMoi); // Trả về 201 Created
}

```

### HTTP Request gửi đi

```http
POST /api/don-hang HTTP/1.1
Host: quan-ca-phe.com
Content-Type: application/json

{
  "tenMon": "Bạc xỉu",
  "luongDuong": "50%",
  "giaTien": 35000
}
```

### HTTP Response trả về (Output)

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 102,
  "tenMon": "Bạc xỉu",
  "luongDuong": "50%",
  "giaTien": 35000,
  "trangThai": "CHO_PHA_CHE"
}
```

## 3. PUT Request

- **Ý nghĩa:** Ghi đè toàn bộ (Replace) dữ liệu của một tài nguyên hiện có bằng dữ liệu mới gửi lên. Có tính chất Idempotent (gọi nhiều lần với cùng dữ liệu thì trạng thái cuối cùng trên Server vẫn không đổi).
- **Ánh xạ Spring Boot:** Sử dụng Annotation `@PutMapping` kết hợp `@PathVariable` (xác định đối tượng bị sửa) và `@RequestBody` (chứa toàn bộ dữ liệu mới).

### Đề bài

Khách hàng muốn thay đổi hoàn toàn đơn hàng `102` từ "Bạc xỉu" sang "Trà đào cam sả".

### Code Spring Boot

```java
@PutMapping("/api/don-hang/{id}")
public ResponseEntity<DonHang> doiToanBoDonHang(
        @PathVariable Long id,
        @RequestBody DonHang donHangThayThe) {

    // 1. Tìm đơn hàng cũ trong Database
    DonHang donHangCoSan = donHangRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng ID: " + id));

    // 2. Ghi đè toàn bộ thông tin mới từ Client gửi lên
    donHangCoSan.setTenMon(donHangThayThe.getTenMon());
    donHangCoSan.setLuongDuong(donHangThayThe.getLuongDuong());
    donHangCoSan.setGiaTien(donHangThayThe.getGiaTien());
    donHangCoSan.setTrangThai("CHO_PHA_CHE"); // Hoặc lấy từ donHangThayThe.getTrangThai() nếu client gửi lên

    // 3. Lưu bản ghi đã bị ghi đè xuống Database
    DonHang donHangDaLuu = donHangRepository.save(donHangCoSan);

    // 4. Trả về kết quả sau khi cập nhật thành công
    return ResponseEntity.ok(donHangDaLuu);
}

```

### HTTP Request gửi đi

```http
PUT /api/don-hang/102 HTTP/1.1
Host: quan-ca-phe.com
Content-Type: application/json

{
  "tenMon": "Trà đào cam sả",
  "luongDuong": "100%",
  "giaTien": 45000
}

```

### HTTP Response trả về (Output)

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 102,
  "tenMon": "Trà đào cam sả",
  "luongDuong": "100%",
  "giaTien": 45000,
  "trangThai": "CHO_PHA_CHE"
}

```

## 4. PATCH Request

- **Ý nghĩa:** Cập nhật một phần (Partial Update) của tài nguyên. Khác với PUT, PATCH chỉ chỉnh sửa đúng các thuộc tính/trường dữ liệu được gửi lên trong Request Body, giữ nguyên tất cả thông tin còn lại.
- **Ánh xạ Spring Boot:** Sử dụng Annotation `@PatchMapping` kết hợp `@PathVariable` và `@RequestBody` (thường hứng dữ liệu dưới dạng `Map<String, Object>` hoặc DTO riêng).

### Đề bài

Khách hàng giữ nguyên món "Trà đào cam sả" ở đơn `102`, chỉ muốn sửa duy nhất trường lượng đường thành "0%".

### Code Spring Boot

```java
@PatchMapping("/api/don-hang/{id}")
public ResponseEntity<DonHang> suaLuongDuong(
        @PathVariable Long id,
        @RequestBody SuaLuongDuongDto dto) {

    // 1. Lấy đơn hàng từ DB
    DonHang donHang = donHangRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

    // 2. Set dữ liệu mới từ DTO vào
    donHang.setLuongDuong(dto.getLuongDuong());

    // 3. Lưu xuống DB và trả về
    return ResponseEntity.ok(donHangRepository.save(donHang));
}
```

### HTTP Request gửi đi

```http
PATCH /api/don-hang/102 HTTP/1.1
Host: quan-ca-phe.com
Content-Type: application/json

{
  "luongDuong": "0%"
}
```

### HTTP Response trả về (Output)

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 102,
  "tenMon": "Trà đào cam sả",
  "luongDuong": "0%",
  "giaTien": 45000,
  "trangThai": "CHO_PHA_CHE"
}
```

## 5. DELETE Request

- **Ý nghĩa:** Yêu cầu Server xóa tài nguyên được chỉ định. Có tính chất Idempotent (xóa 1 lần hay nhiều lần thì tài nguyên đó vẫn biến mất khỏi hệ thống).
- **Ánh xạ Spring Boot:** Sử dụng Annotation `@DeleteMapping` kết hợp `@PathVariable`.

### Đề bài

Khách hàng đổi ý muốn hủy/xóa đơn hàng số `102`.

### Code Spring Boot

```java
@DeleteMapping("/api/don-hang/{id}")
public ResponseEntity<Void> huyDonHang(@PathVariable Long id) {
    // Thực hiện xóa đơn hàng 102 trong Database...
    return ResponseEntity.noContent().build(); // Trả về 204 No Content
}

```

### HTTP Request gửi đi

```http
DELETE /api/don-hang/102 HTTP/1.1
Host: quan-ca-phe.com

```

### HTTP Response trả về (Output)

```http
HTTP/1.1 204 No Content

```

*(Phần Body hoàn toàn rỗng vì tài nguyên đã bị xóa)*

---

### 1.2. Response là gì, Request là gì ?

#### 1.2.1. Request (HTTP Request) là gì?

Request là thông điệp do phía Client (Trình duyệt, App điện thoại, Postman) đóng gói và gửi lên Server (Ứng dụng Spring Boot) để yêu cầu thực hiện một thao tác hoặc lấy dữ liệu.

Một HTTP Request thô bao gồm 3 phần chính:

- Request Line: Chứa HTTP Method (GET, POST, PUT...), URL (Đường dẫn) và HTTP Version.

- Request Headers: Chứa thông tin ngữ cảnh dạng Key-Value (Định dạng dữ liệu Content-Type, Token xác thực Authorization...).

- Request Body: Nội dung dữ liệu gửi kèm (dạng JSON/XML), thường dùng cho POST, PUT, PATCH.

#### 1.2.2. Response (HTTP Response) là gì?

Response là thông điệp do Server (Spring Boot) đóng gói và gửi ngược lại cho Client để trả về kết quả xử lý hoặc thông báo lỗi.

Một HTTP Response thô cũng bao gồm 3 phần chính:

- Status Line: Chứa HTTP Status Code (200, 201, 404, 500...) báo trạng thái xử lý.

- Response Headers: Thông tin về dữ liệu trả về (Kiểu dữ liệu Content-Type: application/json, thông tin Server...).

- Response Body: Dữ liệu thực tế trả về cho Client (Chuỗi JSON, File, HTML...).

## 2. API là gì, RestAPI là gì ?

### 2.1. API là gì? (Application Programming Interface)

API (Giao diện lập trình ứng dụng) là một tập hợp các quy tắc, cơ chế cho phép các phần mềm hoặc hệ thống khác nhau giao tiếp và trao đổi dữ liệu với nhau mà không cần biết cách hệ thống bên trong hoạt động như nào.

### 2.2. REST API là gì?

**REST (Representational State Transfer)** không phải là một công nghệ hay ngôn ngữ, mà là một **kiểu kiến trúc thiết kế** dành cho các ứng dụng web.

**REST API** (hoặc RESTful API) là ứng dụng API tuân thủ các nguyên tắc thiết kế của REST, sử dụng giao thức **HTTP** để trao đổi dữ liệu (thường ở dạng **JSON**).

### 6 Nguyên tắc cốt lõi của REST API

1. **Client - Server tách biệt:** Phân tách rõ ràng giữa giao diện người dùng (Client) và xử lý dữ liệu/kho lưu trữ (Server).
2. **Stateless (Phi trạng thái):** Mỗi Request từ Client gửi lên Server phải chứa đầy đủ thông tin để Server hiểu và xử lý. Server không lưu lại trạng thái (Session) của Client giữa các lần gọi.
3. **Cacheable (Khả năng lưu tạm):** Response trả về phải khai báo rõ dữ liệu đó có được lưu tạm (Cache) ở Client hay không để giảm tải Server.
4. **Uniform Interface (Giao diện chuẩn hóa):** Sử dụng chuẩn URL định danh tài nguyên và dùng đúng các phương thức HTTP (`GET`, `POST`, `PUT`, `DELETE`...).
5. **Layered System (Hệ thống phân lớp):** Client không cần quan tâm đang kết nối trực tiếp với Server thật hay qua các lớp trung gian (Load Balancer, API Gateway, Proxy).
6. **Code on Demand (Tùy chọn):** Server có thể gửi mã thực thi (như JavaScript) về cho Client chạy (ít dùng).

### 2.3. Quy tắc thiết kế chuẩn RESTful API trong Spring Boot

Để thiết kế một hệ thống RESTful API chuẩn trong Spring Boot, bạn cần tuân thủ 3 quy tắc vàng sau:

#### Quy tắc 1: Danh từ chỉ tài nguyên (Nouns, not Verbs)

URL phải đại diện cho **tài nguyên (Resource)** dưới dạng danh từ số nhiều, không dùng động từ trên URL. Hành động được quyết định bởi **HTTP Method**.

- **Sai (Không chuẩn REST):**
- `GET /api/get-all-don-hang`
- `POST /api/create-don-hang`
- `POST /api/delete-don-hang/10`


- **Đúng (Chuẩn RESTful):**
- `GET /api/don-hang` *(Lấy danh sách)*
- `POST /api/don-hang` *(Tạo mới)*
- `DELETE /api/don-hang/10` *(Xóa đơn hàng 10)*

#### Quy tắc 2: Tận dụng đúng HTTP Method & Status Code

- Dùng đúng Method cho hành động (`GET` để đọc, `POST` để tạo, `PUT` để ghi đè, `PATCH` để sửa một phần, `DELETE` để xóa).
- Trả về đúng `HttpStatus` (`200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `404 Not Found`).

#### Quy tắc 3: Sử dụng DTO (Data Transfer Object)

Không trả về trực tiếp `Entity` đại diện cho bảng Database mà dùng `DTO` để ẩn các thông tin nhạy cảm và tối ưu hóa dữ liệu truyền nhận.

### 2.4. So sánh code: Viết API Thường vs Viết chuẩn REST API

Giả sử ứng dụng quản lý Học viên. Cần viết các chức năng: Xem danh sách, Tạo mới, Sửa thông tin, và Xóa học viên.

#### Cách viết API KHÔNG chuẩn REST (Kiểu cũ / RPC style)

*Đặc điểm: Dùng URL chứa ĐỘNG TỪ để mô tả hành động, tất cả đều dùng `GET` hoặc `POST` tùy tiện.*

| Hành động | HTTP Method | Endpoint (URL) | Đánh giá |
| --- | --- | --- | --- |
| Lấy tất cả học viên | `GET` hoặc `POST` | `/api/getAllHocVien` | ❌ Thừa động từ `getAll` |
| Xem chi tiết 1 học viên | `GET` | `/api/getHocVienById?id=5` | ❌ Dùng Query Param rườm rà |
| Thêm mới học viên | `POST` | `/api/createHocVien` | ❌ Thừa động từ `create` |
| Sửa học viên | `POST` | `/api/updateHocVien` | ❌ Dùng sai Method (`POST` thay vì `PUT`/`PATCH`) |
| Xóa học viên | `GET` hoặc `POST` | `/api/deleteHocVien/5` | ❌ Thừa động từ `delete`, dùng `GET` để xóa rất nguy hiểm |

---

#### Cách viết chuẩn REST API (RESTful Style)

*Đặc điểm: URL chỉ chứa DANH TỪ (Tài nguyên). Hành động được quyết định hoàn toàn bởi HTTP Method.*

Tài nguyên ở đây là `hoc-vien` (Danh từ số nhiều).

| Hành động | HTTP Method | Endpoint (URL) | Status Code trả về |
| --- | --- | --- | --- |
| **Lấy tất cả** học viên | `GET` | `/api/v1/hoc-vien` | `200 OK` |
| **Xem chi tiết** học viên ID = 5 | `GET` | `/api/v1/hoc-vien/5` | `200 OK` (hoặc `404` nếu không thấy) |
| **Thêm mới** 1 học viên | `POST` | `/api/v1/hoc-vien` | `201 Created` |
| **Sửa toàn bộ** thông tin ID = 5 | `PUT` | `/api/v1/hoc-vien/5` | `200 OK` |
| **Sửa 1 phần** (ví dụ: đổi SĐT) ID = 5 | `PATCH` | `/api/v1/hoc-vien/5` | `200 OK` |
| **Xóa** học viên ID = 5 | `DELETE` | `/api/v1/hoc-vien/5` | `204 No Content` |

## 3. Design pattern : DI, IOC

### 3.1. IoC (Inversion of Control) — Đảo ngược điều khiển

Khái niệm: Là một tư tưởng/nguyên lý thiết kế phần mềm.

Bản chất: Đảo ngược quyền quản lý đối tượng. Thay vì lập trình viên tự tay khởi tạo (new), cấu hình và tiêu hủy các đối tượng trong code, bạn giao toàn bộ quyền đó cho Spring Framework (IoC Container) tự lo liệu.

### 3.2. DI (Dependency Injection) — Tiêm phụ thuộc

Khái niệm: Là một mẫu thiết kế (Design Pattern) dùng để triển khai thực tế cho tư tưởng IoC.

Bản chất: Hành động Framework tự động tìm, tạo và "tiêm" (đưa) các đối tượng phụ thuộc vào Class đang cần sử dụng chúng (thông qua Constructor, Setter hoặc Field), thay vì Class đó phải tự tìm hay tự new.
