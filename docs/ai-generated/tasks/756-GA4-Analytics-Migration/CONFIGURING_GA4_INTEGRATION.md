# Configuring Google Analytics Integration

Members of the Admin role can use the Google Setup gadget to configure a connection to Google Analytics. This integration allows you to specify a Google Analytics 4 (GA4) Property to use with the Dashboard gadgets.

To add Google Analytics or Google Tag Manager scripts to your websites, the script tag provided by Google must be added to your Template's **Metadata -> Additional Head Content**.

> [!NOTE]
> Percussion CMS has been updated to fully support **Google Analytics 4 (GA4)**. Universal Analytics (UA) is retired and no longer supported. The CMS backend handles all Analytics Data API requests securely using a Service Account.

## Google Service Accounts

A Google Service Account allows Percussion CMS to authenticate with Google's APIs on the backend without requiring manual OAuth user consent. Instead of a username and password, the CMS uses the Service Account's email address and a securely generated `.json` private key file to fetch analytics data for your Dashboard gadgets.

You must register your own project by creating a service account in the Google Cloud Console for your instance of CM1.

## Step 1: Set up a new GA4 Property (If you haven't already)

1. Go to [Google Analytics](https://analytics.google.com).
2. Click **Admin** > **Create Account** (or use an existing account).
3. Create a new **Property** for your website.
4. Name the property something related to your site name and select your reporting time zone and currency.
5. Once created, navigate to **Data Streams**, choose **Web**, and enter your website URL to generate your Measurement ID (e.g., `G-XXXXXXXXXX`). You will use this Measurement ID in your Template's Head Content.

## Step 2: Set up a Service Account in Google Cloud Console

In order to use Google Analytics with CM1's dashboard gadgets, you must configure a Service Account.

1. Log in to the [Google Cloud Console](https://console.cloud.google.com/).
2. Click the **Select a project** dropdown at the top and create a **New Project** (or select an existing one).
3. From the left-hand navigation menu, go to **APIs & Services** > **Library**.
4. Search for and select the **Google Analytics Admin API** and click **Enable**.
5. Search for and select the **Google Analytics Data API** and click **Enable**.
6. From the left-hand navigation menu, select **APIs & Services** > **Credentials**.
7. Click **Create Credentials** -> **Service account**.
8. Provide a Service account name (e.g., `Percussion-CMS-Analytics`) and click **Create and Continue**, then click **Done**.
9. In the Service Accounts list, click the email address of the service account you just created (e.g., `percussion-cms-analytics@your-project.iam.gserviceaccount.com`). *Copy this email address, as you will need it later.*
10. Navigate to the **Keys** tab for the service account.
11. Click **Add Key** -> **Create new key**.
12. Select **JSON** format and click **Create**. The `.json` key file will be downloaded to your computer. Keep this file secure.

## Step 3: Grant the Service Account Access to GA4

Your new Service Account needs permission to read data from your Google Analytics 4 Property.

1. Sign in to [Google Analytics](https://analytics.google.com).
2. Click **Admin**, and navigate to your desired Account and Property.
3. Under the Property column, click **Property Access Management**.
4. Click the blue **+** button in the top right, then click **Add users**.
5. Paste the **Service Account email address** you copied earlier (e.g., `percussion-cms-analytics@your-project.iam.gserviceaccount.com`).
6. Under Standard Roles, grant the **Viewer** or **Analyst** role.
7. Click **Add**.

## Step 4: Adding Google Analytics Reporting to your Site(s)

If you would like to view your website's traffic through the CMS Dashboard, you must configure the Google Setup Gadget.

1. Navigate to the **CM1 Dashboard**.
2. Click **Add Dashboard Gadgets** under the finder.
3. Click and drag **Google Setup** onto the dashboard.
4. Click and drag **Traffic** (or *What's Working*) onto the dashboard.
5. In the Google Setup Gadget, enter your **Google Service Account Email** into the email input field.
6. Using the file browser, select the **.json key file** you downloaded from the Google Cloud Console.
7. Click the **Connect** button to validate your credentials. You will see a message indicating whether the connection was successful.
8. If the connection succeeds, a list of your sites will appear:
   1. Select the correct **Google Analytics Property** from the dropdown menu for each CMS Site.
   2. *(Optional)* If prompted for an API Key, you can enter your GA4 Measurement ID (`G-XXXXXXXXXX`).
9. When you have selected a property for your Site(s), click the **Save** button.

You can now use other tools that rely on the Google Analytics integration, such as the Traffic Gadget (monitors website traffic) and the What's Working Gadget (compares traffic to publish history).

### Removing Google Analytic Tracking

If you no longer want a given site's analytics to be visible in the CMS Dashboard, open the Google Setup Gadget and change the dropdown for your site back to "Select Google Analytics Property", then click Save.

To completely stop tracking traffic on your live site, the Google Analytics scripts added to the *Additional Head Content* in your Pages or Templates must be manually removed, followed by a Full Publish of the site.
