# CameraX Compose App

Aplikasi ini menggunakan **CameraX** dan **Jetpack Compose** untuk menampilkan preview kamera, mengambil foto, menyimpan hasil gambar ke MediaStore, serta menyediakan fitur flash/torch, ganti kamera depan–belakang, dan menampilkan thumbnail foto terakhir.

---

## Izin Kamera

Saat aplikasi dijalankan, aplikasi memeriksa apakah izin kamera sudah diberikan oleh pengguna.  
Jika izin belum diberikan, aplikasi meminta izin menggunakan `ActivityResultContracts.RequestPermission()`.  
Hanya setelah pengguna memberikan izin kamera, CameraX akan diinisialisasi dan preview kamera ditampilkan menggunakan `PreviewView`.  
Jika izin ditolak, kamera tidak dijalankan dan fitur tidak berfungsi.

---

## Penyimpanan Foto Menggunakan MediaStore

Ketika tombol **Ambil Foto** ditekan, aplikasi menangkap gambar melalui `ImageCapture` dan menyimpannya ke penyimpanan publik menggunakan **MediaStore**, sehingga foto langsung muncul di galeri perangkat.

Alur penyimpanan:

1. Membuat `ContentValues` berisi nama file, `MIME_TYPE`, dan lokasi penyimpanan (`Pictures/KameraKu`).
2. Menggunakan `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` untuk menyimpan file ke folder publik.
3. Setelah foto berhasil disimpan, aplikasi menerima `savedUri` dan menampilkannya sebagai thumbnail kecil pada layar.

Dengan pendekatan ini, aplikasi tidak perlu izin penyimpanan karena MediaStore otomatis menangani akses ke folder publik pada perangkat Android modern.

---

## Penanganan Rotasi (Orientation Handling)

CameraX secara otomatis menyesuaikan rotasi berdasarkan orientasi perangkat.  
Saat foto diambil, rotasi target diproses melalui metadata EXIF sehingga hasil foto tetap tampil dengan orientasi yang benar di galeri atau aplikasi lain, meskipun perangkat diputar ketika memotret.

---

## Fitur Utama

- Preview kamera real-time menggunakan CameraX.
- Ambil foto menggunakan `ImageCapture`.
- Simpan otomatis ke folder publik `Pictures/KameraKu`.
- Tampilkan thumbnail foto terakhir.
- Toggle Flash/Torch.
- Ganti kamera depan ↔ belakang.
- Jetpack Compose UI.

---

## Struktur Utama Kode

- `CameraPreview` → Menampilkan `PreviewView` untuk CameraX.
- `CameraScreen` → Mengatur izin, binding kamera, tombol aksi, dan UI Compose.
- `outputOptions()` → Membuat metadata file foto untuk MediaStore.
- `takePhoto()` → Menangkap foto dan menyimpannya.
- `bindPreview()` dan `bindImageCapture()` → Inisialisasi CameraX.