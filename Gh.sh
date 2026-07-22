# 1. احذف كل ملفات Kotlin
rm -rf app/src/main/java/com/apppair

# 2. شغّل السكربت (يُنشئ 10 ملفات صحيحة فقط)
bash setup-app-pair.sh

# 3. تحقق
echo "=== الملفات الموجودة ==="
find app/src -type f -name "*.kt" | sort

# 4. ارفع
git add -A
git commit -m "Complete clean rebuild - 10 files only"
git push --force
