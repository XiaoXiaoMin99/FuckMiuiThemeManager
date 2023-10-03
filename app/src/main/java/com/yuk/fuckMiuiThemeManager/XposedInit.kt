package com.yuk.fuckMiuiThemeManager

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yuk.fuckMiuiThemeManager.utils.callMethod
import com.yuk.fuckMiuiThemeManager.utils.getObjectField
import com.yuk.fuckMiuiThemeManager.utils.setObjectField
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.luckypray.dexkit.DexKitBridge
import miui.drm.DrmManager
import miui.drm.ThemeReceiver
import java.io.File
import java.lang.reflect.Method

private const val TAG = "FuckThemeManager"

class XposedInit : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)
        EzXHelper.initHandleLoadPackage(lpparam)
        when (lpparam.packageName) {
            "android" -> {
                var hook: List<XC_MethodHook.Unhook>? = null
                try {
                    ThemeReceiver::class.java.methodFinder().filterByName("validateTheme").first().createHook {
                        before {
                            hook = DrmManager::class.java.methodFinder().filterByName("isLegal").toList().createHooks {
                                returnConstant(DrmManager.DrmResult.DRM_SUCCESS)
                            }
                        }
                        after {
                            hook?.forEach { it.unhook() }
                        }
                    }
                } catch (t: Throwable) {
                    Log.ex(t)
                }
            }

            "com.android.thememanager" -> {
                try {
                    loadClass("com.android.thememanager.detail.theme.model.OnlineResourceDetail")
                        .methodFinder().filterByName("toResource").toList().createHooks {
                        after {
                            it.thisObject.setObjectField("bought", true)
                        }
                    }
                } catch (t: Throwable) {
                    Log.ex(t)
                }
                try {
                    loadClass("com.android.thememanager.basemodule.views.DiscountPriceView")
                        .methodFinder().filterByParamCount(2)
                        .filterByParamTypes(Int::class.java, Int::class.java).filterByReturnType(Void.TYPE).toList().createHooks {
                            before {
                                it.args[1] = 0
                            }
                        }
                } catch (t: Throwable) {
                    Log.ex(t)
                }
                try {
                    loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().filterByName("themeManagerSupportPaidWidget").first().createHook {
                        returnConstant(false)
                    }
                } catch (t: Throwable) {
                    Log.ex(t)
                }

                System.loadLibrary("dexkit")
                DexKitBridge.create(lpparam.appInfo.sourceDir)?.use { bridge ->
                    val map = mapOf(
                        "DrmResult" to setOf(
                            "theme",
                            "ThemeManagerTag",
                            "/system",
                            "check rights isLegal:"
                        ),
                        "LargeIcon" to setOf(
                            "apply failed",
                            "/data/system/theme/large_icons/",
                            "default_large_icon_product_id",
                            "largeicons",
                            "relativePackageList is empty"
                        ),
                    )

                    val resultMap = bridge.batchFindMethodsUsingStrings {
                        queryMap(map)
                    }

                    val drmResult = resultMap["DrmResult"]!!
                    assert(drmResult.size == 1)
                    val drmResultDescriptor = drmResult.first()
                    val drmResultMethod: Method =
                        drmResultDescriptor.getMethodInstance(lpparam.classLoader)
                    drmResultMethod.createHook {
                        after {
                            it.result = DrmManager.DrmResult.DRM_SUCCESS
                        }
                    }

                    val largeIcon = resultMap["LargeIcon"]!!
                    assert(largeIcon.size == 1)
                    val largeIconDescriptor = largeIcon.first()
                    val largeIconMethod: Method =
                        largeIconDescriptor.getMethodInstance(lpparam.classLoader)
                    largeIconMethod.createHook {
                        before {
                            val resource = it.thisObject.javaClass.fieldFinder()
                                .filterByType(
                                    loadClass(
                                        "com.android.thememanager.basemodule.resource.model.Resource",
                                        lpparam.classLoader
                                    )
                                ).first()
                            val productId = it.thisObject.getObjectField(resource.name)
                                ?.callMethod("getProductId").toString()
                            val strPath =
                                "/storage/emulated/0/Android/data/com.android.thememanager/files/MIUI/theme/.data/rights/theme/${productId}-largeicons.mra"
                            val file = File(strPath)
                            val fileParent = file.parentFile!!
                            if (!fileParent.exists()) fileParent.mkdirs()
                            file.createNewFile()
                        }
                    }
                }
            }

            "com.miui.personalassistant" -> {
                try {
                    loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().filterByName("themeManagerSupportPaidWidget").first().createHook {
                        returnConstant(false)
                    }

                    loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel")
                        .methodFinder().filterByName("isCanDirectAddMaMl").first()
                        .createHook {
                            returnConstant(true)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailDownloadManager\$Companion")
                        .methodFinder()
                        .filterByName("isCanDownload").first().createHook {
                            returnConstant(true)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailUtil")
                        .methodFinder().filterByName("isCanAutoDownloadMaMl").first()
                        .createHook {
                            returnConstant(true)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse")
                        .methodFinder().filterByName("isPay").first().createHook {
                        returnConstant(false)
                    }

                    loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse")
                        .methodFinder().filterByName("isBought").first()
                        .createHook {
                            returnConstant(true)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper")
                        .methodFinder().filterByName("isPay").first()
                        .createHook {
                            returnConstant(false)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper")
                        .methodFinder().filterByName("isBought").first()
                        .createHook {
                            returnConstant(true)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel")
                        .methodFinder().filterByName("shouldCheckMamlBoughtState")
                        .first().createHook {
                            returnConstant(false)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel")
                        .methodFinder()
                        .filterByName("isTargetPositionMamlPayAndDownloading").first().createHook {
                            returnConstant(false)
                        }

                    loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel")
                        .methodFinder()
                        .filterByName("checkIsIndependentProcessWidgetForPosition").first().createHook {
                            returnConstant(true)
                        }
                } catch (t: Throwable) {
                    Log.ex(t)
                }
            }

            "com.miui.home" -> {
                try {
                    loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().filterByName("themeManagerSupportPaidWidget").first().createHook {
                        returnConstant(false)
                    }

                    loadClass("com.miui.home.launcher.gadget.MaMlPendingHostView").methodFinder().filterByName("isCanAutoStartDownload").first().createHook {
                        returnConstant(true)
                    }
                } catch (t: Throwable) {
                    Log.ex(t)
                }
            }
        }
    }

}