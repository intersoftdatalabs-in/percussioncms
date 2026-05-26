# GA4 Analytics Reconfiguration Guide

**Important Note for Site Administrators:**
Due to Google sunsetting Universal Analytics (UA) on July 1, 2024, the Percussion CMS Google Analytics integration has been upgraded to support Google Analytics 4 (GA4).

This upgrade introduces a **breaking change** to existing analytics configurations.

## What Changed?

In Universal Analytics, data was organized by **Accounts > Properties (Web Properties) > Views (Profiles)**.
In Google Analytics 4, data is simplified to **Accounts > Properties**. There are no longer "Profiles" or "Views".

Because the CMS internally stored your configuration using the old UA Profile ID (e.g., `12345678|12345`), the analytics dashboard will no longer be able to fetch data until you reconfigure your site to select a **GA4 Property**.

## How to Reconfigure Your Site

To restore your analytics dashboard, please follow these steps:

### Step 1: Create a Google Cloud Service Account (JSON Key)

The new GA4 API requires a Service Account JSON key. (Note: Old `.p12` keys used for Universal Analytics are no longer supported by the GA4 API.)

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Select your project or create a new one.
3. Navigate to **IAM & Admin > Service Accounts**.
4. Click **Create Service Account**, provide a name, and click **Done**.
5. Click on the newly created Service Account, go to the **Keys** tab.
6. Click **Add Key > Create new key**, select **JSON**, and click **Create**. The JSON file will download to your computer.
7. Open the JSON file in a text editor. Note the `client_email` value (e.g., `my-service-account@my-project.iam.gserviceaccount.com`).

### Step 2: Grant Access in Google Analytics

1. Go to your [Google Analytics Admin Dashboard](https://analytics.google.com/).
2. Select your GA4 Account and Property.
3. Click on **Property Access Management**.
4. Add the `client_email` from your JSON key with at least **Viewer** permissions.

### Step 3: Update Percussion CMS Configuration

1. Log into your Percussion CMS instance.
2. Navigate to the **Administration** dashboard and find the **Google Analytics** settings.
3. Update the **Service Account Email** to match the `client_email` from your JSON file.
4. Upload or paste the contents of your new **JSON Key File** (replacing the old configuration).
5. Once authenticated, the **Profile** dropdown will now populate with your **GA4 Properties** (e.g., `My Website Data (properties/123456789)`).
6. Select the correct GA4 Property from the dropdown.
7. Save your configuration.

Your analytics dashboard should now successfully render data using Google Analytics 4.
