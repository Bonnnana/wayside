using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Wayside.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddStoredPlacePhotos : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "PhotosJson",
                table: "Places",
                type: "TEXT",
                nullable: false,
                defaultValue: "");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "PhotosJson",
                table: "Places");
        }
    }
}
