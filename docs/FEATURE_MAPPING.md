# Frontend-to-backend feature mapping

| CareFinder feature | Backend implementation | Angular responsibility |
|---|---|---|
| 100-hospital directory | Seeded MySQL hospital, specialty and insurer tables | Render cards and load-more UI |
| Search and filters | `/hospitals` query parameters | Build query from filter controls |
| GPS radius and nearest sort | Haversine distance filtering/sorting | Ask browser for coordinates |
| Hospital profile | `/hospitals/{id}` | Render profile and Leaflet map |
| Google directions | Latitude/longitude returned in hospital DTO | Open Google Maps URL |
| Insurance network | `/insurers` and hospital `insurance[]` | Provider dropdown and badges |
| Emergency/24x7 | Hospital search flags and chatbot intent | Emergency buttons and 112 link |
| Compare up to 3 | `/hospitals/compare` | Selection tray and comparison page |
| PDF comparison | `/hospitals/compare/report.pdf` | Trigger browser download |
| Register/login/logout | `/auth/**`, BCrypt, JWT and refresh rotation | Forms and token storage |
| Forgot password | One-time reset token flow | Reset forms; email adapter is deployment work |
| Profile | `/users/me` | Profile screen |
| Favorites | `/me/favorites/**` | Heart button and favorites page |
| Recently viewed | Auto-record on authenticated detail GET plus `/me/recently-viewed/**` | Recent page |
| Chatbot | `/chatbot/messages`, directory-backed EN/HI answers and cards | Chat window and quick questions |
| Voice input | Chat API accepts resulting text | Web Speech API/microphone UI |
| Hindi/English switch | Chat request `language: en | hi` | UI labels and language toggle |
| Chat history | `/me/chat-history` for signed-in users | Browser UI/optional offline copy |
| PWA/offline | Public GET responses are safe for network-first caching | Angular service worker and install prompt |
| Admin management | Hospital, insurer, user, analytics and audit APIs | Future admin dashboard |
| Hospital staff | Assigned-hospital update API with ownership check | Future staff dashboard |

Insurance **network participation** is included. A separate insurance-plan
comparison module is intentionally not included.
