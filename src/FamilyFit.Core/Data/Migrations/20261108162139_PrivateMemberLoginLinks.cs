using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace FamilyFit.Core.Data.Migrations
{
    /// <inheritdoc />
    public partial class PrivateMemberLoginLinks : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<Guid>(
                name: "ExpectedMemberId",
                table: "LoginChallenges",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "MemberLoginLinkId",
                table: "LoginChallenges",
                type: "TEXT",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "MemberLoginLinks",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "TEXT", nullable: false),
                    MemberId = table.Column<Guid>(type: "TEXT", nullable: false),
                    TokenHash = table.Column<string>(type: "TEXT", nullable: false),
                    BrowserSecretHash = table.Column<string>(type: "TEXT", nullable: true),
                    ExpiresAtUtc = table.Column<long>(type: "INTEGER", nullable: false),
                    UsedAtUtc = table.Column<long>(type: "INTEGER", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_MemberLoginLinks", x => x.Id);
                    table.ForeignKey(
                        name: "FK_MemberLoginLinks_Members_MemberId",
                        column: x => x.MemberId,
                        principalTable: "Members",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateIndex(
                name: "IX_LoginChallenges_MemberLoginLinkId",
                table: "LoginChallenges",
                column: "MemberLoginLinkId");

            migrationBuilder.CreateIndex(
                name: "IX_MemberLoginLinks_MemberId",
                table: "MemberLoginLinks",
                column: "MemberId");

            migrationBuilder.CreateIndex(
                name: "IX_MemberLoginLinks_TokenHash",
                table: "MemberLoginLinks",
                column: "TokenHash",
                unique: true);

            migrationBuilder.AddForeignKey(
                name: "FK_LoginChallenges_MemberLoginLinks_MemberLoginLinkId",
                table: "LoginChallenges",
                column: "MemberLoginLinkId",
                principalTable: "MemberLoginLinks",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_LoginChallenges_MemberLoginLinks_MemberLoginLinkId",
                table: "LoginChallenges");

            migrationBuilder.DropTable(
                name: "MemberLoginLinks");

            migrationBuilder.DropIndex(
                name: "IX_LoginChallenges_MemberLoginLinkId",
                table: "LoginChallenges");

            migrationBuilder.DropColumn(
                name: "ExpectedMemberId",
                table: "LoginChallenges");

            migrationBuilder.DropColumn(
                name: "MemberLoginLinkId",
                table: "LoginChallenges");
        }
    }
}
