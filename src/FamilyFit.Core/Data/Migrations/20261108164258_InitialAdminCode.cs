using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace FamilyFit.Core.Data.Migrations
{
    /// <inheritdoc />
    public partial class InitialAdminCode : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AlterColumn<Guid>(
                name: "DeviceId",
                table: "BrowserSessions",
                type: "TEXT",
                nullable: true,
                oldClrType: typeof(Guid),
                oldType: "TEXT");

            migrationBuilder.CreateTable(
                name: "AdminBootstraps",
                columns: table => new
                {
                    Id = table.Column<int>(type: "INTEGER", nullable: false),
                    MemberId = table.Column<Guid>(type: "TEXT", nullable: false),
                    CompletedAtUtc = table.Column<long>(type: "INTEGER", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_AdminBootstraps", x => x.Id);
                    table.ForeignKey(
                        name: "FK_AdminBootstraps_Members_MemberId",
                        column: x => x.MemberId,
                        principalTable: "Members",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateIndex(
                name: "IX_AdminBootstraps_MemberId",
                table: "AdminBootstraps",
                column: "MemberId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            // The previous schema requires a phone. Close only the initial phone-less sessions on rollback.
            migrationBuilder.Sql("DELETE FROM BrowserSessions WHERE DeviceId IS NULL;");
            migrationBuilder.DropTable(
                name: "AdminBootstraps");

            migrationBuilder.AlterColumn<Guid>(
                name: "DeviceId",
                table: "BrowserSessions",
                type: "TEXT",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"),
                oldClrType: typeof(Guid),
                oldType: "TEXT",
                oldNullable: true);
        }
    }
}
