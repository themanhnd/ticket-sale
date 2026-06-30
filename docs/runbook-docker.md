# Runbook Docker

## Mục tiêu

File này ghi cách chạy stack local/dev/prod bằng Docker Compose mà không commit secret lên git.

## Quy ước file env

- `.env.example`: danh sách biến chung cần biết.
- `.env.dev.example`: mẫu cho môi trường dev/local.
- `.env.prod.example`: mẫu cho production.
- `.env`: giá trị thật đang dùng trên máy/server, không commit.
- `.env.dev`, `.env.prod`: giá trị thật theo môi trường, không commit.

## Chạy local/dev

Tạo `.env` từ mẫu dev:

```powershell
Copy-Item .env.dev.example .env
```

Chạy stack:

```powershell
docker compose --profile platform up -d --build
```

Kiểm tra:

```powershell
docker compose --profile platform ps
curl.exe http://localhost:8080/actuator/health
```

## Chạy production bằng Compose

Tạo `.env` trên server từ mẫu prod:

```bash
cp .env.prod.example .env
```

Sửa các giá trị secret thật trong `.env`:

```bash
MYSQL_ROOT_PASSWORD=...
JWT_SECRET=...
ENCRYPT_KEY=...
```

Chạy stack:

```bash
docker compose --profile platform up -d --build
```

## Lưu ý

- Không commit `.env`, `.env.dev`, `.env.prod`.
- Chỉ commit các file `*.example`.
- Nếu đổi `MYSQL_ROOT_PASSWORD` sau khi DB đã có volume cũ, MySQL có thể vẫn dùng password cũ trong volume.
- Muốn reset DB local thì dùng `docker compose down -v`, thao tác này xóa dữ liệu MySQL local.
