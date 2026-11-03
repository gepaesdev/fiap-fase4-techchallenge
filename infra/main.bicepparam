// Parameters for infra/main.bicep. No personal data or secrets here: e-mail addresses are read from
// environment variables at deploy time (ADMIN_EMAILS, and optionally ALERT_EMAIL).
using './main.bicep'

param location = 'brazilsouth'
param environmentName = 'dev'

param adminEmails = readEnvironmentVariable('ADMIN_EMAILS')
param alertEmail = readEnvironmentVariable('ALERT_EMAIL', readEnvironmentVariable('ADMIN_EMAILS'))

// 1 = previous full week (production). Set REPORT_WEEK_OFFSET=0 to demo the current week.
param reportWeekOffset = int(readEnvironmentVariable('REPORT_WEEK_OFFSET', '1'))

param budgetAmount = 5
param enableBudget = true
// Fixed on purpose: Azure does not allow changing a budget's start date, so a date computed from the
// current month would break every redeploy made in a later month.
param budgetStartDate = '2026-10-01'

// Team members' Microsoft Entra object ids (az ad user show --id <email> --query id -o tsv).
param teamMemberPrincipalIds = []
param teamMemberRole = 'Reader'
