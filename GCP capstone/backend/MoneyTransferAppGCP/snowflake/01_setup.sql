-- ============================================================
-- Money Transfer DW — Step 01: Setup
-- Database, Warehouse, Schema
-- Run as: ACCOUNTADMIN or SYSADMIN
-- ============================================================

-- -------------------------------------------------------
-- 1. Virtual Warehouse
-- -------------------------------------------------------
CREATE WAREHOUSE IF NOT EXISTS COMPUTE_WH
    WAREHOUSE_SIZE   = 'X-SMALL'
    AUTO_SUSPEND     = 60          -- suspend after 60 s idle
    AUTO_RESUME      = TRUE
    INITIALLY_SUSPENDED = TRUE
    COMMENT = 'Compute warehouse for Money Transfer DW analytics';

-- -------------------------------------------------------
-- 2. Database
-- -------------------------------------------------------
CREATE DATABASE IF NOT EXISTS MONEY_TRANSFER_DW
    COMMENT = 'Money Transfer data warehouse';

-- -------------------------------------------------------
-- 3. Schema
-- -------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS MONEY_TRANSFER_DW.ANALYTICS
    COMMENT = 'Analytics schema — dimension & fact tables';

-- -------------------------------------------------------
-- 4. Activate context
-- -------------------------------------------------------
USE WAREHOUSE COMPUTE_WH;
USE DATABASE  MONEY_TRANSFER_DW;
USE SCHEMA    ANALYTICS;
