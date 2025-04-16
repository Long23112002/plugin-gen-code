# Entity Generator Plugin

Plugin cho IntelliJ IDEA giúp tạo nhanh các thành phần code liên quan đến Entity trong ứng dụng Spring Boot.

## Tính năng

- Tạo DTO với khả năng chọn các trường
- Tạo Repository với các phương thức findBy
- Tạo Service với các phương thức CRUD
- Tạo Controller với các endpoint REST
- Tạo EntityParam cho tìm kiếm và phân trang
- Hỗ trợ nhiều mẫu kiến trúc khác nhau thông qua cấu hình

## Cài đặt

1. Mở IntelliJ IDEA
2. Vào `Settings/Preferences` > `Plugins`
3. Chọn biểu tượng bánh răng và chọn `Install Plugin from Disk`
4. Chọn file plugin-gen-code-1.0.0.zip
5. Khởi động lại IDE

## Sử dụng

1. Mở một lớp Entity (có chú thích @Entity)
2. Chuột phải và chọn "Generate Entity Code" hoặc sử dụng phím tắt Alt+G
3. Chọn các thành phần muốn tạo (DTO, Service, Repository, Controller, Filter)
4. Chọn các trường cho DTO và Filter
5. Nhấn OK để tạo code

## Các thành phần được tạo ra

### DTO

- Tạo lớp DTO với các trường được chọn
- Tạo getter/setter hoặc sử dụng Lombok @Data

### Repository

- Tạo interface mở rộng JpaRepository
- Tạo các phương thức findBy cho các trường được chọn làm filter
- Tạo phương thức tìm kiếm kết hợp (ví dụ: findByNameAndGender)

### Service

- Tạo lớp Service với các phương thức CRUD cơ bản
- Tạo phương thức search với EntityParam
- Tất cả các phương thức trả về Entity

### Controller

- Tạo controller với đầy đủ các endpoint REST
- Hỗ trợ tìm kiếm với EntityParam
- Endpoint cho tìm kiếm với phân trang

### EntityParam

- Tạo lớp parameter dùng cho tìm kiếm
- Tự động hỗ trợ phân trang với Page và Size
- Tạo các trường tương ứng với trường được chọn làm filter

## Hỗ trợ

Nếu bạn gặp vấn đề hoặc có ý tưởng cải tiến, vui lòng tạo issue tại [GitHub repository](https://github.com/yourusername/plugin-gen-code).

## License

MIT License
