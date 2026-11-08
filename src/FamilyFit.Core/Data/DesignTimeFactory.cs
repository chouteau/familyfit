using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace FamilyFit.Core.Data;

public sealed class DesignTimeFactory : IDesignTimeDbContextFactory<FamilyDb>
{
    public FamilyDb CreateDbContext(string[] args) => new(new DbContextOptionsBuilder<FamilyDb>().UseSqlite("Data Source=familyfit-design.db").Options);
}
