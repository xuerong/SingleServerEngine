/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50719
 Source Host           : localhost
 Source Database       : test

 Target Server Type    : MySQL
 Target Server Version : 50719
 File Encoding         : utf-8

 Date: 01/16/2018 07:52:16 AM
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `unlimitedStarAward`
-- ----------------------------
DROP TABLE IF EXISTS `unlimitedStarAward`;
CREATE TABLE `unlimitedStarAward` (
  `userId` varchar(255) NOT NULL,
  `star` int(11) NOT NULL,
  `resetTime` bigint(20) DEFAULT NULL,
  `award` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
