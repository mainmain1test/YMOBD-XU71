package com.example.data.obd

import com.example.data.models.EcuConfigCategory
import com.example.data.models.EcuConfigItem

object EcuConfigService {
    // No ECU-specific routine is sent until the exact ECU software and a trusted
    // service definition are verified. Generic ELM327 support alone is insufficient.

    val defaultConfigs: List<EcuConfigItem> = listOf(
        EcuConfigItem(
            id = "cfg_factory_alarm",
            titleFa = "آژیر و بوق هشدار دزدگیر فابریک",
            titleEn = "Factory Anti-Theft Siren & Horn Chirp",
            descriptionFa = "فعال‌سازی تک بوق و آژیر فابریک خودرو هنگام قفل و باز کردن با ریموت، و آلارم در صورت باز شدن غیرمجاز درب‌ها از طریق نود BCM/CCN.",
            category = EcuConfigCategory.ALARM_SECURITY,
            isEnabled = false,
            supportedOnElm = false,
            commandRoutine = "",
            safetyNotice = "ایمن و استاندارد؛ تنظیمات در حافظه EEPROM نود شبکه خودرو (BCM/CCN) ذخیره می‌گردد و نیازی به باز کردن سیم‌کشی ندارد.",
            currentValueDisplay = "نیازمند تأیید سازگاری ECU"
        ),
        EcuConfigItem(
            id = "cfg_auto_lock_20km",
            titleFa = "قفل خودکار درب‌ها در سرعت ۲۰ کیلومتر",
            titleEn = "Speed Sensing Auto Door Lock (20 km/h)",
            descriptionFa = "با رسیدن سرعت خودرو به ۲۰ کیلومتر بر ساعت، نود مرکزی بدنه تمامی قفل‌های درب‌ها را به صورت خودکار قفل می‌کند.",
            category = EcuConfigCategory.COMFORT_BCM,
            isEnabled = false,
            supportedOnElm = false,
            commandRoutine = "",
            safetyNotice = "کاملاً ایمن؛ آپشن استاندارد ایران‌خودرو است که در صورت تصادف شدید قفل‌ها به طور خودکار باز می‌شوند.",
            currentValueDisplay = "نیازمند تأیید سازگاری ECU"
        ),
        EcuConfigItem(
            id = "cfg_fan_temp_xu7",
            titleFa = "تنظیم و کالیبراسیون دمای روشن شدن فن رادیاتور (ویژه موتور XU7)",
            titleEn = "Cooling Fan Temperature Threshold (XU7 Protection)",
            descriptionFa = "در صورت پشتیبانی دقیق ECU، آستانه‌های روشن‌شدن فن را تغییر می‌دهد. مقدارهای نمایش‌داده‌شده باید قبل از نوشتن با ECU واقعی خودرو تأیید شوند.",
            category = EcuConfigCategory.COOLING_FAN,
            isEnabled = false,
            supportedOnElm = false,
            commandRoutine = "",
            safetyNotice = "قبل از نوشتن، شناسه و نرم‌افزار ECU باید تأیید شود. تغییر آستانه فن بدون تأیید کالیبراسیون خودرو توصیه نمی‌شود.",
            currentValueDisplay = "نیازمند تأیید سازگاری ECU"
        ),
        EcuConfigItem(
            id = "cfg_stepper_reset",
            titleFa = "ریست و کالیبراسیون استپر موتور و دریچه گاز",
            titleEn = "Stepper Motor & Throttle Body Adaptation Reset",
            descriptionFa = "پاک کردن مقادیر یادگیری شده پله‌های استپر موتور زیمنس و صفر کردن موقعیت سنسور دریچه گاز (TPS) جهت رفع گاز هرز خوردن و نوسان دور آرام.",
            category = EcuConfigCategory.THROTTLE_IDLE,
            isEnabled = false,
            supportedOnElm = false,
            commandRoutine = "",
            safetyNotice = "پس از اجرای این دستور، سوییچ را به مدت ۱۰ ثانیه بسته و مجدداً بدون فشردن پدال گاز استارت بزنید.",
            currentValueDisplay = "نیازمند تأیید سازگاری ECU"
        ),
        EcuConfigItem(
            id = "cfg_ecu_adaptation_reset",
            titleFa = "فلش و ریست حافظه موقت ECU زیمنس / کروز",
            titleEn = "Siemens ECU Learned Adaptation Reset (LTFT/STFT)",
            descriptionFa = "پاک کردن حافظه تطبیقی سنسور اکسیژن، خطاهای ماندگار و زمان‌بندی پاشش انژکتورها برای بازگشت به تنظیمات کارخانه‌ای اولیه.",
            category = EcuConfigCategory.RESET_ADAPTATION,
            isEnabled = false,
            supportedOnElm = false,
            commandRoutine = "",
            safetyNotice = "ایمن؛ موتور پس از ریست حدود ۵ دقیقه برای یادگیری نسبت بهینه سوخت کار خواهد کرد.",
            currentValueDisplay = "نیازمند تأیید سازگاری ECU"
        ),
        EcuConfigItem(
            id = "cfg_follow_me_home",
            titleFa = "چراغ همراهی تا درب منزل (Follow-Me-Home)",
            titleEn = "Follow-Me-Home Headlight Delay (30s)",
            descriptionFa = "روشن ماندن نور پایین چراغ‌های جلو به مدت ۳۰ ثانیه پس از خاموش کردن خودرو و قفل درب‌ها در پارکینگ یا محیط‌های تاریک.",
            category = EcuConfigCategory.COMFORT_BCM,
            isEnabled = false,
            supportedOnElm = false,
            commandRoutine = "",
            safetyNotice = "پس از ۳۰ ثانیه جهت حفظ شارژ باتری به صورت خودکار خاموش می‌شود.",
            currentValueDisplay = "نیازمند تأیید سازگاری ECU"
        ),
        EcuConfigItem(
            id = "cfg_overspeed_buzzer",
            titleFa = "بوق هشدار سرعت ۱۲۰ کیلومتر",
            titleEn = "120 km/h Overspeed Warning Chime",
            descriptionFa = "فعال یا غیرفعال کردن بیپ هشدار صوتی پشت آمپر در هنگام عبور از سرعت ۱۲۰ کیلومتر بر ساعت.",
            category = EcuConfigCategory.COMFORT_BCM,
            isEnabled = false,
            supportedOnElm = false,
            commandRoutine = "",
            safetyNotice = "تنها آلارم صوتی پشت آمپر را کنترل می‌کند و تاثیری در شتاب خودرو ندارد.",
            currentValueDisplay = "نیازمند تأیید سازگاری ECU"
        )
    )
}
