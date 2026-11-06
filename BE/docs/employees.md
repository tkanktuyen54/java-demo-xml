# Employee API

Module `modules/employee` tham khảo các luồng trong `C:\Delta\transport-erp`:

- `src/Modules/HRM/TransportERP.Modules.HRM.Api/Employees`: list và detail hồ sơ đã duyệt.
- `src/Modules/HRM/TransportERP.Modules.HRM.Api/EmployeeRequests`: phiếu tạo/sửa, nháp, gửi duyệt, revision và thu hồi/hủy.
- `docs/modules/hrm/implement/employee-requests.md`: full-state target và áp dụng đúng revision được duyệt.

Bản Java dùng kiến trúc hiện có `controller → service → repository`, JDBC và Flyway. Phạm vi gồm hồ sơ cá nhân, ngày làm việc, vị trí/phòng ban ban đầu, liên hệ, địa chỉ và liên hệ khẩn cấp; workflow duyệt một cấp trong một công ty. Bộ máy workflow nhiều cấp, tenant/đơn vị làm việc, Fuel, DMS, ngân hàng, bằng cấp, kỹ năng, người phụ thuộc, cấp tài khoản employee và nghiệp vụ nghỉ việc/tái tuyển của ERP chưa nằm trong module này. HTTP response và cách đánh phiên bản được thiết kế cho BE Java, không tương thích nguyên trạng với contract ERP.

DTO đầu vào nằm trong `dto/request`, DTO đầu ra nằm trong `dto/response`, mỗi record một file. `EmployeeProfile` dùng chung cho input, output và snapshot. Các enum nghiệp vụ `EmployeeStatus`, `EmployeeRequestType`, `EmployeeRequestState` nằm trong `entity`. Việc tách DTO giữ nguyên các field JSON và endpoint; tên schema DTO trong OpenAPI theo tên lớp mới.

## Quyền và đăng nhập

Tất cả endpoint yêu cầu đăng nhập bằng session hiện có. Các lệnh POST, PUT, DELETE yêu cầu CSRF: gọi `GET /api/auth/csrf`, gửi cookie và header `X-XSRF-TOKEN`. Lấy lại CSRF sau login/register/logout.

| Quyền | Đọc employee và phiếu đã gửi | Lập/sửa/gửi/thu hồi/hủy/xóa phiếu của mình | Duyệt/trả phiếu |
| --- | --- | --- | --- |
| STUDENT | Không | Không | Không |
| HR | Có | Có | Không |
| HR_APPROVER | Có | Không | Có |
| ADMIN | Có | Có | Có |

Người lập không được tự duyệt/trả phiếu, kể cả ADMIN. Nháp chỉ người lập được đọc và xuất hiện trên danh sách của họ. Tài khoản đăng ký công khai chỉ có STUDENT; request body không thể tự cấp quyền HR.

Migration V4 thêm `account_role`. Quản trị viên database cấp quyền cho các tài khoản đã đăng ký, ví dụ:

```sql
INSERT INTO account_role (user_id, role)
SELECT id, 'HR' FROM tbl_user WHERE email = 'hr@example.com';

INSERT INTO account_role (user_id, role)
SELECT id, 'HR_APPROVER' FROM tbl_user WHERE email = 'approver@example.com';
```

Đăng nhập lại sau khi đổi quyền. Một tài khoản có thể có nhiều quyền. API đăng ký không liên kết hoặc tạo employee.

## Endpoint

Base URL khi chạy local: `http://localhost:8080`; Docker BE: `http://localhost:8081`. Swagger: `/swagger-ui.html`.

| Method | Route | Kết quả |
| --- | --- | --- |
| GET | `/api/employees` | Trang hồ sơ đã duyệt |
| GET | `/api/employees/{id}` | `{id, employeeCode, status, profile, version, createdAtUtc, lastModifiedAtUtc}` |
| GET | `/api/employee-requests` | Trang phiếu tạo/sửa |

ID employee trên API là **UUID**. Field `id` trong employee list/detail và `subjectId` trong input/output của phiếu tạo/sửa đều dùng cùng UUID, ví dụ `550e8400-e29b-41d4-a716-446655440000`. ID của phiếu, revision và tài khoản vẫn là số.

```text
GET /api/employees/550e8400-e29b-41d4-a716-446655440000
```

Ví dụ mở phiếu sửa:

```json
{
  "subjectId": "550e8400-e29b-41d4-a716-446655440000"
}
```

`subjectId` lấy từ employee list/detail hoặc response phiếu đã duyệt. API không nhận employee ID số cũ: UUID sai định dạng trả 400, UUID hợp lệ nhưng không tồn tại trả 404. Client cần chuyển sang dùng UUID cho employee URL và `subjectId`.

Database vẫn giữ `employee.id` BIGINT làm khóa chính và khóa ngoại. UUID nằm trong `employee.public_id` CHAR(36), NOT NULL và UNIQUE; BE sinh bằng `UUID.randomUUID()` khi duyệt tạo employee. Client không tự cấp UUID. Migration V5 cấp UUID cho employee đã có, giữ nguyên mã employee, profile, version, khóa ngoại và lịch sử phiếu. Update hồ sơ giữ nguyên UUID.

Khởi động lại BE để Flyway áp dụng V5, không cần tạo lại database. Kiểm tra bằng:

```sql
SELECT id AS internal_id, public_id, employee_code, full_name
FROM employee;
```

List employee nhận `page=1`, `pageSize=20` (1–100), `search`, `status`, `department`, `sortBy=employeeCode`, `sortDirection=asc`. Search từ 2–200 ký tự, hoặc rỗng; tìm tên, mã, email và điện thoại, không diễn giải `%`/`_` thành wildcard. `status`: Active/Suspended/Terminated. Các sort: employeeCode, fullName, department, position, status, createdAtUtc; hướng asc/desc. List chỉ trả thông tin tóm tắt, không trả dữ liệu định danh cá nhân.

List phiếu nhận `page`, `pageSize`, `search` theo tên/mã employee, `type=Creation|Update`, `state` và `includeCompleted=false`. Mặc định loại phiếu Approved; dùng `includeCompleted=true` để xem cả phiếu đã duyệt. Trang trả `{items, totalCount, page, pageSize, totalPages}`; lọc, đếm và phân trang trong database.

Hai family dùng chung các endpoint sau:

- `/api/employee-creation-requests`: tạo employee.
- `/api/employee-update-requests`: sửa hồ sơ employee.

| Method | Route tương đối trong family | Body / hành vi |
| --- | --- | --- |
| POST | `/` | `{target?}` cho tạo; `{subjectId, target?}` cho sửa; trả 201 Draft |
| POST | `/submissions` | `{target, reason}`; sửa thêm `subjectId`; tạo và gửi trong một transaction, trả 201 |
| GET | `/{id}` | Chi tiết phiếu, baseline, target, version, latestRevisionId |
| PUT | `/{id}/draft` | `{version, target}`; thay toàn bộ target |
| POST | `/{id}/submit` | `{version, reason, target?}`; đóng băng revision mới, chuyển PendingApproval |
| POST | `/{id}/approve` | `{version, revisionId, reason?}`; duyệt và áp dụng hồ sơ trong cùng transaction |
| POST | `/{id}/reject` | `{version, revisionId, reason}`; trả phiếu để sửa |
| POST | `/{id}/revoke` | `{version, reason?}`; người lập thu hồi phiếu PendingApproval |
| POST | `/{id}/cancel` | `{version, reason}`; hủy phiếu PendingApproval/Rejected/Revoked |
| DELETE | `/{id}?version=...` | Xóa mềm phiếu Draft/Rejected/Revoked/Cancelled; giữ revision và audit |
| GET | `/{id}/revisions` | Các bản đã gửi, từ cũ tới mới |
| GET | `/{id}/revisions/{revisionId}` | Đúng baseline, target, lý do và người gửi của bản đó |
| GET | `/{id}/activity-history?page=1&pageSize=20` | Lịch sử thao tác/người thực hiện/lý do, mới nhất trước |

## Target mẫu và quy trình

Gọi `POST /api/employee-creation-requests` với:

```json
{
  "target": {
    "fullName": "Nguyễn Văn An",
    "avatar": null,
    "personalInformation": {
      "dateOfBirth": "1990-05-20",
      "gender": "Male",
      "nationality": "Việt Nam",
      "citizenIdNumber": "012345678901"
    },
    "employment": {
      "hireDate": "2026-11-01",
      "probationStartDate": "2026-11-01",
      "probationEndDate": "2026-12-31",
      "officialDate": "2027-01-01"
    },
    "assignment": {
      "department": "Vận hành",
      "position": "Tài xế",
      "workingType": "FullTime",
      "effectiveFrom": "2026-11-01"
    },
    "contacts": [
      {
        "type": "Work",
        "email": "an@example.com",
        "phoneNumber": "0901234567",
        "isPrimary": true
      }
    ],
    "addresses": [],
    "emergencyContacts": [],
    "note": null
  }
}
```

1. Response trả `id`, `state=Draft`, `version=1`. Có thể tạo phiếu Creation với `{}` rồi nhập target sau. Tạo phiếu Update với `{subjectId}` tự sao chép hồ sơ hiện tại làm baseline và target.
2. Gửi `POST /{id}/submit` với `{"version":1,"reason":"Tuyển mới tài xế"}`. Response trả PendingApproval, version mới và latestRevisionId. Chưa có thay đổi trong employee.
3. Tài khoản HR_APPROVER khác người lập gọi `POST /{id}/approve` với version mới và `revisionId=latestRevisionId`. Response trả Approved và subjectId. Dùng subjectId để đọc `/api/employees/{subjectId}`.
4. Khi sửa, tạo phiếu Update, sửa target lấy từ response, lưu nháp hoặc gửi target mới kèm lý do. Hồ sơ chính thức vẫn giữ nguyên cho đến khi được duyệt.
5. Rejected hoặc Revoked có thể gửi lại với lý do mới. PUT draft từ hai trạng thái đó chuyển sang Draft riêng tư; mọi revision cũ vẫn giữ nguyên. Hủy kết thúc phiếu. Nháp chưa gửi dùng DELETE để bỏ.

## Quy tắc dữ liệu và chống ghi đè

- `personalInformation`, `employment`, `contacts`, `addresses`, `emergencyContacts` phải có khi target khác null. Các collection là full replacement; `[]` xóa hết nội dung của collection, không phải PATCH. Optional scalar null xóa giá trị đó.
- Nháp được thiếu dữ liệu nghiệp vụ. Khi gửi và khi duyệt phải có fullName, hireDate, assignment đầy đủ, ít nhất một contact có email hoặc điện thoại và đúng một primary cho mỗi loại contact có mặt.
- Contact type: Work/Personal/Other; gender: Male/Female/Other; workingType: FullTime/PartTime/Contract/Intern; address type: Permanent/Temporary/Other. Address có fullAddress hoặc addressLine1 và một primary cho mỗi loại. EmergencyContact có fullName, relationship, phoneNumber; đúng một primary nếu danh sách không rỗng.
- Kiểm tra thứ tự ngày sinh, CCCD, nhận việc, thử việc, chính thức và ngày phân công. Email trim/lowercase, không trùng trong hồ sơ hoặc với employee khác. Unique index là chốt cuối khi có cạnh tranh.
- Mã employee tự sinh dạng `EMP-000001`; status khi tạo là Active. Update không đổi mã, status hoặc assignment. Điều chuyển/nghỉ việc cần luồng nghiệp vụ riêng.
- Version phiếu tăng sau **mọi** lệnh thành công; luôn dùng version từ response gần nhất, không tái sử dụng version cũ. Đây là version chung của phiếu, khác draftVersion=0/correction mechanics của ERP.
- Submit đóng băng baseline, target và lý do trong revision bất biến. Quyết định phải chỉ đúng latestRevisionId đang PendingApproval. Thu hồi/trả phiếu không sửa revision cũ.
- Duyệt áp dụng đúng target trong revision và ghi audit trong một transaction. Khóa dòng và kiểm tra version ngăn hai lần duyệt tạo hai employee.
- Update ghi nhận baselineVersion lúc mở nháp. Nếu hồ sơ được phiếu khác cập nhật trước khi gửi hoặc duyệt, trả 409 `employee_profile_changed_create_new_request`; người lập tạo phiếu mới từ hồ sơ hiện tại.
- GET không tạo nháp hay thay đổi dữ liệu. Xóa mềm giữ chứng cứ; API thường trả 404 cho phiếu đã xóa.
- Lỗi: 400 dữ liệu/query không hợp lệ, 401 chưa đăng nhập, 403 thiếu quyền/khác người lập/tự duyệt, 404 không tìm thấy hoặc nháp của người khác, 409 sai version/trạng thái/revision/email trùng/hồ sơ đã đổi. Lỗi nghiệp vụ dùng `{message: "error_code"}`.

Hồ sơ đầy đủ và snapshots lưu dưới dạng JSON có DTO typed; các cột tìm kiếm và bảng contact được cập nhật cùng transaction để phân trang và bảo vệ email unique. V5 thêm UUID, không sửa các migration V1–V4 đã áp dụng.

## Kiểm thử

Để chạy một luồng có dữ liệu lưu thật trong database `khoa`, bật MySQL và backend local theo README, rồi chạy từ thư mục gốc:

```sh
node scripts/employee-demo.mjs http://127.0.0.1:8080
```

Script tạo hai tài khoản demo riêng (HR và HR_APPROVER), cấp quyền qua MySQL Docker và đăng nhập lại bằng cookie/CSRF thật. Luồng gồm tạo nháp, gửi duyệt, kiểm tra HR không được tự duyệt, người khác duyệt tạo nhân viên, sửa số điện thoại, gửi và duyệt cập nhật. Script kiểm tra hồ sơ chỉ đổi sau khi duyệt, UUID/mã nhân viên giữ nguyên, version tăng từ 1 lên 2 và audit ghi đúng người duyệt. Dữ liệu mẫu được giữ lại; mỗi lần chạy tạo một nhân viên và hai tài khoản mới. Kết quả, ID phiếu và thông tin đăng nhập demo lưu trong `BE/target/employee-demo-result.json` (ngoài Git).

JDK 21, từ thư mục BE:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd test
```

`EmployeeIntegrationTests` kiểm tra bằng đăng nhập/session thật: tạo/sửa qua duyệt, privacy, phân quyền/CSRF, trả và nộp lại, revision, thu hồi/hủy/xóa mềm, rollback, email unique, baseline stale, tìm kiếm/phân trang, OpenAPI và hai lệnh duyệt đồng thời. Các luồng dùng UUID, kiểm tra ID nhất quán giữa employee/phiếu và giữ quan hệ BIGINT nội bộ. `EmployeeUuidMigrationTests` kiểm tra nâng schema V4 có dữ liệu lên V5, giữ nguyên profile/version/khóa ngoại/audit, UUID không thay đổi khi chạy lại và ràng buộc NOT NULL/UNIQUE. Mặc định test chạy H2 MySQL mode; có thể override datasource bằng system properties để chạy bộ test API trên database MySQL riêng.
