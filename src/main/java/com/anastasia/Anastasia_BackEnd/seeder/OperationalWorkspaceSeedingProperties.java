package com.anastasia.Anastasia_BackEnd.seeder;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.operational-seeding")
public class OperationalWorkspaceSeedingProperties {

    private boolean enabled = false;
    private boolean reset = false;
    private String tenantSlug = "anastasis-staging";
    private String tenantDisplayName = "Anastasis Staging";
    private String tenantOwnerName = "Anastasis User";
    private String tenantOwnerEmail = "anastasis-user@anastasis.local";
    private String tenantOwnerPhone = "+1 555 010 4100";
    private String tenantBillingEmail = "billing@anastasis.local";
    private String tenantTimezone = "America/New_York";
    private String tenantLocale = "en";
    private String tenantCountryCode = "US";
    private String ownerPassword = "ChangeMe123!";
    private String ownerAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=512&q=80";
    private String ownerProfileLocation = "New York, USA";
    private String churchName = "Anastasis Church";
    private String churchNameLocal = "Anastasis Church";
    private String churchPrefix = "St.";
    private String churchPrefixLocal = "St.";
    private String churchEmail = "anastasis-church@anastasis.local";
    private String churchPhone = "+1 555 010 4101";
    private String churchTimezone = "America/New_York";
    private String churchLocale = "en-US";
    private String churchDenomination = "Orthodox";
    private String churchDescription = "Operational workspace used for deployment verification and tenant-scoped staging or production checks.";
    private String churchDescriptionLocal = "Operational workspace used for deployment verification and tenant-scoped staging or production checks.";
    private String churchWebsite = "https://anastasisapp.com";
    private String churchInstagram = "https://instagram.com/anastasisapp";
    private String churchYoutube = "https://youtube.com/@anastasisapp";
    private String churchFacebook = "https://facebook.com/anastasisapp";
    private String churchNeighborhood = "Midtown";
    private String churchNeighborhoodLocal = "Midtown";
    private String churchDiocese = "Northeast Diocese";
    private String churchDioceseLocal = "Northeast Diocese";
    private String churchAddressLine1 = "100 Anastasis Way";
    private String churchAddressLine2 = "Suite 100";
    private String churchCity = "New York";
    private String churchStateProvince = "NY";
    private String churchPostalCode = "10001";
    private String churchCountry = "USA";
    private String churchGpsLocation = "https://maps.google.com/?q=40.7484,-73.9857";
    private Double churchLatitude = 40.7484;
    private Double churchLongitude = -73.9857;
    private String churchAvatarUrl = "https://images.unsplash.com/photo-1518733057094-95b53143d2a7?auto=format&fit=crop&w=1024&q=80";
}
