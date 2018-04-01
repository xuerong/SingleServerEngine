﻿// Classes and structures being serialized

// Generated by ProtocolBuffer
// - a pure c# code generation implementation of protocol buffers
// Report bugs to: https://silentorbit.com/protobuf/

// DO NOT EDIT
// This file will be overwritten when CodeGenerator is run.
// To make custom modifications, edit the .proto file and add //:external before the message line
// then write the code and the changes in a separate file.
using System;
using System.Collections.Generic;

namespace Example
{
    /// <summary>
    /// <para>option java_outer_classname = "AccountPB";</para>
    /// <para>10001</para>
    /// </summary>
    public partial class CSLogin
    {
        public string AccountId { get; set; }

        public string Url { get; set; }

        public string Ip { get; set; }

        public string Localization { get; set; }

        /// <summary> 本地化，语言</summary>
        public string ClientVersion { get; set; }

    }

    public partial class SCLogin
    {
        public string SessionId { get; set; }

        public long ServerTime { get; set; }

    }

    /// <summary> 玩家信息</summary>
    public partial class CSUserInfo
    {
    }

    public partial class SCUserInfo
    {
        public string Id { get; set; }

        public string Name { get; set; }

        public string Icon { get; set; }

    }

    public partial class CSChangeUserInfo
    {
        public string Name { get; set; }

        public string Icon { get; set; }

    }

    public partial class SCChangeUserInfo
    {
    }

    public partial class CSLogout
    {
        public string AccountId { get; set; }

    }

    public partial class SCLogout
    {
    }

    /// <summary> 被顶下</summary>
    public partial class SCBeTakePlace
    {
    }

    /// <summary>
    /// <para> 获取登陆信息，</para>
    /// <para> 1、当第一次登陆的时候，获取自己要在哪里注册</para>
    /// <para> 2、前端清理了缓存，需要重新获取自己账号所在服务器</para>
    /// </summary>
    public partial class CSGetLoginInfo
    {
        public string DeviceId { get; set; }

    }

    public partial class SCGetLoginInfo
    {
        public int ServerId { get; set; }

        public string Ip { get; set; }

        public int Port { get; set; }

        public string AccountId { get; set; }

        public int ServerState { get; set; }

    }

}
