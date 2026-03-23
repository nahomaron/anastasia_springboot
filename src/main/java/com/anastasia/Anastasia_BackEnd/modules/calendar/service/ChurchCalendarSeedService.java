package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntrySourceType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarRecurrenceEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.RecurrenceFrequency;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChurchCalendarSeedService {

    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final Set<CalendarCategory> CELEBRATION_CATEGORIES = Set.of(CalendarCategory.LITURGY, CalendarCategory.WORSHIP);
    private static final Set<CalendarCategory> COMMEMORATION_CATEGORIES = Set.of(CalendarCategory.LITURGY);

    private final CalendarEntryRepository calendarEntryRepository;
    private final GeezCalendarSupport geezCalendarSupport;

    @Transactional
    public void seedDefaults(TenantEntity tenant, ChurchEntity church, UserEntity owner) {
        if (tenant == null || church == null) {
            return;
        }

        GeezCalendarSupport.GeezDate todayGeez = geezCalendarSupport.toGeezFromGregorian(LocalDate.now(ZoneOffset.UTC));
        Instant now = Instant.now();

        MONTHLY_COMMEMORATIONS.forEach(definition ->
                upsertMonthlyCommemoration(tenant, church, owner, todayGeez.year(), now, definition));
        YEARLY_CELEBRATIONS.forEach(definition ->
                upsertYearlyCelebration(tenant, church, owner, todayGeez.year(), now, definition));
    }

    private void upsertMonthlyCommemoration(
            TenantEntity tenant,
            ChurchEntity church,
            UserEntity owner,
            int geezYear,
            Instant now,
            MonthlyCommemorationSeed definition
    ) {
        UUID sourceId = sourceId(definition.code());
        CalendarEntryEntity entry = calendarEntryRepository.findByTenantIdAndSourceEntityId(tenant.getId(), sourceId)
                .orElseGet(CalendarEntryEntity::new);

        LocalDate startDate = geezCalendarSupport.toGregorianFromGeez(geezYear, 1, definition.geezDay());
        populateBaseEntry(
                entry,
                tenant,
                church,
                owner,
                now,
                sourceId,
                CalendarEntryType.COMMEMORATION,
                definition.nameEn(),
                definition.description(),
                COMMEMORATION_CATEGORIES,
                startDate
        );

        CalendarRecurrenceEntity recurrence = getOrCreateRecurrence(entry);
        recurrence.setFrequency(RecurrenceFrequency.MONTHLY);
        recurrence.setInterval(1);
        recurrence.setCalendarSystem(CalendarSystem.GEEZ);
        recurrence.setGeezMonth(null);
        recurrence.setGeezDay(definition.geezDay());
        recurrence.setByMonth(new HashSet<>());
        recurrence.setByMonthDay(new HashSet<>(Set.of(definition.geezDay())));
        recurrence.setByDay(new HashSet<>());
        recurrence.setUntil(null);
        recurrence.setCount(null);

        entry.setDescription(buildDescription(definition.description(), definition.nameTi()));
        entry.setRecurrence(recurrence);
        calendarEntryRepository.save(entry);
    }

    private void upsertYearlyCelebration(
            TenantEntity tenant,
            ChurchEntity church,
            UserEntity owner,
            int geezYear,
            Instant now,
            YearlyCelebrationSeed definition
    ) {
        UUID sourceId = sourceId(definition.code());
        CalendarEntryEntity entry = calendarEntryRepository.findByTenantIdAndSourceEntityId(tenant.getId(), sourceId)
                .orElseGet(CalendarEntryEntity::new);

        LocalDate startDate = geezCalendarSupport.toGregorianFromGeez(geezYear, definition.geezMonth(), definition.geezDay());
        populateBaseEntry(
                entry,
                tenant,
                church,
                owner,
                now,
                sourceId,
                CalendarEntryType.CELEBRATION,
                definition.nameEn(),
                definition.type(),
                CELEBRATION_CATEGORIES,
                startDate
        );

        CalendarRecurrenceEntity recurrence = getOrCreateRecurrence(entry);
        recurrence.setFrequency(RecurrenceFrequency.YEARLY);
        recurrence.setInterval(1);
        recurrence.setCalendarSystem(CalendarSystem.GEEZ);
        recurrence.setGeezMonth(definition.geezMonth());
        recurrence.setGeezDay(definition.geezDay());
        recurrence.setByMonth(new HashSet<>());
        recurrence.setByMonthDay(new HashSet<>());
        recurrence.setByDay(new HashSet<>());
        recurrence.setUntil(null);
        recurrence.setCount(null);

        entry.setDescription(buildDescription(definition.type(), definition.nameTi()));
        entry.setRecurrence(recurrence);
        calendarEntryRepository.save(entry);
    }

    private void populateBaseEntry(
            CalendarEntryEntity entry,
            TenantEntity tenant,
            ChurchEntity church,
            UserEntity owner,
            Instant now,
            UUID sourceId,
            CalendarEntryType type,
            String title,
            String description,
            Set<CalendarCategory> categories,
            LocalDate startDate
    ) {
        entry.setTenantId(tenant.getId());
        entry.setChurch(church);
        entry.setOwnerUser(owner);
        entry.setType(type);
        entry.setTitle(title);
        entry.setDescription(description);
        entry.setCalendarSystem(CalendarSystem.GEEZ);
        entry.setStartAtUtc(startDate.atStartOfDay().toInstant(ZoneOffset.UTC));
        entry.setEndAtUtc(null);
        entry.setTimezone(DEFAULT_TIMEZONE);
        entry.setAllDay(true);
        entry.setVisibility(CalendarVisibility.PUBLIC);
        entry.setStatus(CalendarEntryStatus.SCHEDULED);
        entry.setStatusChangedAt(now);
        entry.setSourceEntityType(CalendarEntrySourceType.MANUAL);
        entry.setSourceEntityId(sourceId);
        entry.setCategories(new HashSet<>(categories));
        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(now);
        }
        entry.setUpdatedAt(now);
        if (owner != null) {
            if (entry.getCreatedBy() == null) {
                entry.setCreatedBy(owner.getUuid());
            }
            entry.setUpdatedBy(owner.getUuid());
        }
    }

    private CalendarRecurrenceEntity getOrCreateRecurrence(CalendarEntryEntity entry) {
        CalendarRecurrenceEntity recurrence = entry.getRecurrence();
        if (recurrence == null) {
            recurrence = new CalendarRecurrenceEntity();
            recurrence.setEntry(entry);
        }
        return recurrence;
    }

    private UUID sourceId(String code) {
        return UUID.nameUUIDFromBytes(("church-calendar:" + code).getBytes(StandardCharsets.UTF_8));
    }

    private String buildDescription(String primary, String tigrinyaName) {
        if (tigrinyaName == null || tigrinyaName.isBlank()) {
            return primary;
        }
        return primary + " | " + tigrinyaName;
    }

    private static int geezMonth(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "meskerem" -> 1;
            case "tikimt" -> 2;
            case "hidar" -> 3;
            case "tahsas" -> 4;
            case "ter" -> 5;
            case "yekatit" -> 6;
            case "megabit" -> 7;
            case "miyazya" -> 8;
            case "ginbot" -> 9;
            case "sene" -> 10;
            case "hamle" -> 11;
            case "nehase" -> 12;
            case "pagumen" -> 13;
            default -> throw new IllegalArgumentException("Unsupported Geez month: " + value);
        };
    }

    private record MonthlyCommemorationSeed(
            String code,
            int geezDay,
            String nameEn,
            String nameTi,
            String description
    ) {
    }

    private record YearlyCelebrationSeed(
            String code,
            int geezMonth,
            int geezDay,
            String nameEn,
            String nameTi,
            String type
    ) {
    }

    private static final List<MonthlyCommemorationSeed> MONTHLY_COMMEMORATIONS = List.of(
            new MonthlyCommemorationSeed("LIDETA_MARIAM", 1, "Lideta Mariam", "ልደታ ማርያም", "Nativity of the Virgin Mary"),
            new MonthlyCommemorationSeed("ABUNE_GEBRE_MENFES_KIDUS", 5, "Abune Gebre Menfes Kidus", "አቡነ ገብረ መንፈስ ቅዱስ", "Egyptian Hermit (Abo)"),
            new MonthlyCommemorationSeed("KIDUS_SELASSIE", 7, "Kidus Selassie", "ቅዱስ ስላሴ", "Holy Trinity"),
            new MonthlyCommemorationSeed("KIDUS_MESKEL", 10, "Kidus Meskel", "ቅዱስ መስቀል", "Feast of the Holy Cross"),
            new MonthlyCommemorationSeed("ARCH_MICHAEL", 12, "Kidus Michael", "ቅዱስ ሚካኤል", "Archangel Michael"),
            new MonthlyCommemorationSeed("KIDUS_RUPHAEL", 13, "Kidus Ruphael", "ቅዱስ ሩፋኤል", "Archangel Raphael"),
            new MonthlyCommemorationSeed("ABUNE_AREGAWI", 14, "Abune Aregawi", "አቡነ አረጋዊ", "One of the Nine Saints"),
            new MonthlyCommemorationSeed("KIDANE_MIHRET_MONTHLY", 16, "Kidane Mihret", "ኪዳነ ምሕረት", "Covenant of Mercy"),
            new MonthlyCommemorationSeed("KIDUS_GABRIEL", 19, "Kidus Gabriel", "ቅዱስ ገብርኤል", "Archangel Gabriel"),
            new MonthlyCommemorationSeed("KIDIST_MARIAM", 21, "Kidist Mariam", "ቅድስት ማርያም", "Commemoration of the Virgin Mary"),
            new MonthlyCommemorationSeed("KIDUS_GIORGIS", 23, "Kidus Giorgis", "ቅዱስ ጊዮርጊስ", "St. George the Martyr"),
            new MonthlyCommemorationSeed("ABUNE_TEKLE_HAYMANOT", 24, "Abune Tekle Haymanot", "አቡነ ተክለ ሃይማኖት", "Famous Ethiopian Saint"),
            new MonthlyCommemorationSeed("MEDHANE_ALEM", 27, "Medhane Alem", "መድኃኔ ዓለም", "Savior of the World"),
            new MonthlyCommemorationSeed("BEALE_WOLD", 29, "Be'ale Wold", "በዓለ ወልድ", "Feast of God the Son")
    );

    private static final List<YearlyCelebrationSeed> YEARLY_CELEBRATIONS = List.of(
            new YearlyCelebrationSeed("MESKEREM_1_NEW_YEAR", geezMonth("Meskerem"), 1, "Enkutatash / Kudus Yohannes", "ቅዱስ ዮሃንስ / ሓድሽ ዓመት", "New Year"),
            new YearlyCelebrationSeed("MESKEREM_17_MESKEL", geezMonth("Meskerem"), 17, "Meskel", "መስቀል", "Finding of the True Cross"),
            new YearlyCelebrationSeed("TAHSAS_29_LIDET", geezMonth("Tahsas"), 29, "Lidet (Gena)", "ልደት", "Christmas"),
            new YearlyCelebrationSeed("TER_11_TIMKET", geezMonth("Ter"), 11, "Timket", "ጥምቀት", "Epiphany"),
            new YearlyCelebrationSeed("YEKATIT_16_KIDANE_MIHRET", geezMonth("Yekatit"), 16, "Kidane Mihret (Annual)", "ዓመት በዓል ኪዳነ ምሕረት", "Annual Covenant of Mercy"),
            new YearlyCelebrationSeed("NEHASE_16_FILSETA", geezMonth("Nehase"), 16, "Filseta (Assumption)", "ፍልሰታ ማርያም", "Assumption of the Virgin Mary")
    );
}
