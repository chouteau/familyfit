using Microsoft.EntityFrameworkCore;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Core.Data;

public sealed class FamilyDb(DbContextOptions<FamilyDb> options) : DbContext(options)
{
    protected override void ConfigureConventions(ModelConfigurationBuilder configurationBuilder)
    {
        configurationBuilder.Properties<DateTimeOffset>().HaveConversion<UtcTicksConverter>();
    }
    public DbSet<Member> Members => Set<Member>();
    public DbSet<WeightEntry> Weights => Set<WeightEntry>();
    public DbSet<Location> Locations => Set<Location>();
    public DbSet<Exercise> Exercises => Set<Exercise>();
    public DbSet<WorkoutProgram> Programs => Set<WorkoutProgram>();
    public DbSet<ProgramDay> Days => Set<ProgramDay>();
    public DbSet<WorkoutResult> Results => Set<WorkoutResult>();
    public DbSet<Device> Devices => Set<Device>();
    public DbSet<Invitation> Invitations => Set<Invitation>();
    public DbSet<LoginChallenge> LoginChallenges => Set<LoginChallenge>();
    public DbSet<BrowserSession> BrowserSessions => Set<BrowserSession>();

    protected override void OnModelCreating(ModelBuilder b)
    {
        b.Entity<Member>().Property(x => x.Revision).IsConcurrencyToken();
        b.Entity<Location>().Property(x => x.Revision).IsConcurrencyToken();
        b.Entity<WorkoutProgram>().HasMany(x => x.Days).WithOne().HasForeignKey(x => x.ProgramId).OnDelete(DeleteBehavior.Cascade);
        b.Entity<WorkoutProgram>().HasIndex(x => new { x.MemberId, x.WeekStart, x.State });
        b.Entity<WorkoutResult>().HasIndex(x => new { x.MemberId, x.ProgramDayId });
        b.Entity<Device>().HasIndex(x => x.TokenHash).IsUnique();
        b.Entity<Invitation>().HasIndex(x => x.CodeHash).IsUnique();
        b.Entity<LoginChallenge>().HasIndex(x => x.CodeHash).IsUnique();
        b.Entity<Location>().HasOne<Member>().WithMany().HasForeignKey(x => x.MemberId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<WeightEntry>().HasOne<Member>().WithMany().HasForeignKey(x => x.MemberId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<Device>().HasOne<Member>().WithMany().HasForeignKey(x => x.MemberId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<Invitation>().HasOne<Member>().WithMany().HasForeignKey(x => x.MemberId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<WorkoutProgram>().HasOne<Member>().WithMany().HasForeignKey(x => x.MemberId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<ProgramDay>().HasOne<Location>().WithMany().HasForeignKey(x => x.LocationId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<WorkoutResult>().HasOne<Member>().WithMany().HasForeignKey(x => x.MemberId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<WorkoutResult>().HasOne<ProgramDay>().WithMany().HasForeignKey(x => x.ProgramDayId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<BrowserSession>().HasOne<Device>().WithMany().HasForeignKey(x => x.DeviceId).OnDelete(DeleteBehavior.Restrict);
    }
}

public sealed class UtcTicksConverter() : Microsoft.EntityFrameworkCore.Storage.ValueConversion.ValueConverter<DateTimeOffset, long>(
    value => value.UtcTicks, value => new DateTimeOffset(value, TimeSpan.Zero));
