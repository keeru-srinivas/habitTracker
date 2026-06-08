# habitTracker (monorepo)

| Path | Contents |
|------|----------|
| **`backend/`** | FastAPI server, deploy, nginx config, **`LICENSE`**, **`README.md`** — API docs: Swagger **`/docs`**, ReDoc **`/redoc`**. |
| **`HabitTracker App/`** | Android client. |

**Production API:** `https://habit.thatinsaneguy.com` · **Swagger:** `https://habit.thatinsaneguy.com/docs`

Deploy from **`backend/`**:

```bash
cd backend
sudo ./deploy.sh
```

Details: **`backend/README.md`** (auth, habits, weekly ISO weeks, CORS, curl examples).
