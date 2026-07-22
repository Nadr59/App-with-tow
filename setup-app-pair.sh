# انسخ هذا الأمر كاملاً والصقه في Terminal

rm -f app/src/main/java/com/apppair/di/AppModule.kt && \
rm -f app/src/main/java/com/apppair/data/model/PermissionStatus.kt && \
rm -f app/src/main/java/com/apppair/data/model/SelectedAppPair.kt && \
rm -f app/src/main/java/com/apppair/ui/viewmodel/MainViewModel.kt && \
rm -f app/src/main/java/com/apppair/ui/viewmodel/AppSelectionViewModel.kt && \
rm -f app/src/main/java/com/apppair/ui/selection/AppSelectionScreen.kt && \
rm -f app/src/main/java/com/apppair/ui/navigation/AppNavigation.kt && \
rm -f app/src/main/java/com/apppair/ui/navigation/AppNavRoot.kt && \
rm -f app/src/main/java/com/apppair/ui/permissions/PermissionsScreen.kt && \
rm -f app/src/main/java/com/apppair/ui/permissions/PermissionsViewModel.kt && \
rm -f app/src/main/java/com/apppair/ui/oem/OemBatteryGuideScreen.kt && \
rm -f app/src/main/java/com/apppair/utils/NotificationHelper.kt && \
rm -f app/src/main/java/com/apppair/utils/FloatingWidgetController.kt && \
rm -f app/src/main/java/com/apppair/service/AppPairForegroundService.kt && \
rm -f app/src/main/res/drawable/ic_launcher_foreground.xm && \
rmdir app/src/main/java/com/apppair/di 2>/dev/null; \
rmdir app/src/main/java/com/apppair/ui/viewmodel 2>/dev/null; \
rmdir app/src/main/java/com/apppair/ui/selection 2>/dev/null; \
rmdir app/src/main/java/com/apppair/ui/navigation 2>/dev/null; \
rmdir app/src/main/java/com/apppair/ui/permissions 2>/dev/null; \
rmdir app/src/main/java/com/apppair/ui/oem 2>/dev/null; \
sed -i 's/nm\.areNotificationsEnabled$/nm.areNotificationsEnabled()/' app/src/main/java/com/apppair/utils/PermissionUtils.kt && \
echo "=== DONE ===" && \
find app/src -type f -name "*.kt" | sort
